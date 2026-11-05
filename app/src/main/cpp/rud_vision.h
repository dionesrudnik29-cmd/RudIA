#pragma once

#include <atomic>
#include <functional>
#include <string>

namespace rud {

// Lê uma imagem com um modelo de visão pequeno (SmolVLM) e devolve, pedaço a
// pedaço, a descrição gerada. Carrega o modelo, descreve e libera a memória
// logo em seguida, para não disputar RAM com a IA principal.
//
// rgb: width * height * 3 bytes (RGBRGB...).
// prompt: deve conter o marcador "<__media__>" no lugar da imagem.
// on_piece: recebe cada pedaço de texto (UTF-8); devolver false interrompe.
// Retorna string vazia em caso de sucesso, ou a mensagem de erro.
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
    const std::function<bool(const char*, int)>& on_piece);

}  // namespace rud
