#include "rud_vision.h"

#include <llama.h>
#include <mtmd.h>
#include <mtmd-helper.h>

#include <algorithm>
#include <vector>

namespace rud {

namespace {

struct VisionSession {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    mtmd_context* mctx = nullptr;
    mtmd_bitmap* bitmap = nullptr;
    mtmd_input_chunks* chunks = nullptr;
    llama_sampler* sampler = nullptr;
    llama_batch batch{};
    bool has_batch = false;

    ~VisionSession() {
        if (has_batch) llama_batch_free(batch);
        if (sampler) llama_sampler_free(sampler);
        if (chunks) mtmd_input_chunks_free(chunks);
        if (bitmap) mtmd_bitmap_free(bitmap);
        if (mctx) mtmd_free(mctx);
        if (ctx) llama_free(ctx);
        if (model) llama_model_free(model);
    }
};

void replace_all(std::string& s, const std::string& from, const std::string& to) {
    if (from.empty() || from == to) return;
    size_t pos = 0;
    while ((pos = s.find(from, pos)) != std::string::npos) {
        s.replace(pos, from.size(), to);
        pos += to.size();
    }
}

}  // namespace

std::string describe_image(
    const std::string& model_path,
    const std::string& mmproj_path,
    const unsigned char* rgb,
    int width,
    int height,
    const std::string& prompt,
    int max_tokens,
    int n_threads,
    const std::atomic<bool>& stop,
    const std::function<bool(const char*, int)>& on_piece) {

    if (!rgb || width <= 0 || height <= 0) return "A imagem está vazia ou corrompida.";
    const int threads = std::max(1, n_threads);

    VisionSession s;

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    s.model = llama_model_load_from_file(model_path.c_str(), mp);
    if (!s.model) return "Não foi possível abrir o leitor de imagens. Baixe-o novamente.";

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 512;
    cp.n_ubatch = 512;
    cp.n_threads = threads;
    cp.n_threads_batch = threads;
    s.ctx = llama_init_from_model(s.model, cp);
    if (!s.ctx) return "Memória insuficiente para analisar a imagem. Feche outros aplicativos.";

    mtmd_context_params mparams = mtmd_context_params_default();
    mparams.use_gpu = false;
    mparams.print_timings = false;
    mparams.n_threads = threads;
    mparams.warmup = false;
    s.mctx = mtmd_init_from_file(mmproj_path.c_str(), s.model, mparams);
    if (!s.mctx) return "Não foi possível iniciar o leitor de imagens. Baixe-o novamente.";
    if (!mtmd_support_vision(s.mctx)) return "O leitor de imagens instalado não suporta fotos.";

    if (stop.load()) return "";

    s.bitmap = mtmd_bitmap_init((uint32_t) width, (uint32_t) height, rgb);
    if (!s.bitmap) return "Não foi possível preparar a imagem.";

    std::string text = prompt;
    replace_all(text, "<__media__>", mtmd_default_marker());

    mtmd_input_text input;
    input.text = text.c_str();
    input.text_len = text.size();
    input.add_special = false;  // o prompt já traz o token inicial do modelo
    input.parse_special = true;

    s.chunks = mtmd_input_chunks_init();
    const mtmd_bitmap* bitmaps[] = {s.bitmap};
    if (mtmd_tokenize(s.mctx, s.chunks, &input, bitmaps, 1) != 0) {
        return "Não foi possível preparar a imagem para a IA.";
    }

    const int n_ctx = (int) llama_n_ctx(s.ctx);
    const int n_prompt = (int) mtmd_helper_get_n_pos(s.chunks);
    if (n_prompt >= n_ctx - 16) return "A imagem é grande demais para o leitor de imagens.";
    const int budget = std::min(max_tokens, n_ctx - n_prompt - 1);

    llama_pos n_past = 0;
    if (mtmd_helper_eval_chunks(s.mctx, s.ctx, s.chunks, 0, 0, (int32_t) cp.n_batch, true, &n_past) != 0) {
        return "Erro ao analisar a imagem (memória insuficiente?).";
    }

    const llama_vocab* vocab = llama_model_get_vocab(s.model);
    // Modelos pequenos tendem a repetir frases: penalidade leve + escolha gulosa.
    s.sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(s.sampler, llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.15f, 0.0f, 0.0f));
    llama_sampler_chain_add(s.sampler, llama_sampler_init_greedy());

    s.batch = llama_batch_init(1, 0, 1);
    s.has_batch = true;

    std::vector<char> piece(256);
    std::string produced;
    for (int i = 0; i < budget; ++i) {
        if (stop.load()) break;
        llama_token token = llama_sampler_sample(s.sampler, s.ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        // special=false: tokens de controle (ex.: <end_of_utterance>) não aparecem no texto.
        int len = llama_token_to_piece(vocab, token, piece.data(), (int32_t) piece.size(), 0, false);
        if (len < 0) {
            piece.resize((size_t) -len);
            len = llama_token_to_piece(vocab, token, piece.data(), (int32_t) piece.size(), 0, false);
        }
        if (len > 0) {
            produced.append(piece.data(), (size_t) len);
            // Se o arquivo não marcar o fim do turno, o modelo começaria a
            // inventar a próxima fala do usuário: paramos aqui.
            if (produced.find("User:") != std::string::npos) break;
            if (!on_piece(piece.data(), len)) break;
        }

        s.batch.n_tokens = 1;
        s.batch.token[0] = token;
        s.batch.pos[0] = n_past++;
        s.batch.n_seq_id[0] = 1;
        s.batch.seq_id[0][0] = 0;
        s.batch.logits[0] = 1;
        if (llama_decode(s.ctx, s.batch) != 0) {
            return "Erro durante a análise da imagem (memória insuficiente?).";
        }
    }
    return "";
}

}  // namespace rud
