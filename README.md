# Rud IA

Assistente de IA que roda 100% no celular (Android), usando o modelo local
**Qwen3-1.7B Q4_K_M (GGUF, ~1,1 GB)** com llama.cpp. Sem respostas simuladas.

## Como pegar o APK

1. Abra a aba **Actions** do repositório.
2. Entre na execução mais recente com o ✅ verde.
3. Em **Artifacts**, baixe **RudIA-apk** (é um .zip).
4. Abra o .zip no celular e toque em `app-release.apk`.
5. Permita "instalar apps desconhecidos" se o Android pedir.

## Primeira vez

- O app abre e procura o modelo sozinho.
- Se não encontrar, toque em **Baixar modelo** (use o Wi-Fi). Se cair, o
  download continua de onde parou.
- Se já tiver o arquivo `Qwen3-1.7B-Q4_K_M.gguf` no celular, use **Importar .gguf**.
- Depois disso, o modelo carrega automaticamente toda vez que o app abre.

## Fotos e emojis

- A Rud IA responde usando emojis de forma natural.
- Toque no **+** ao lado do campo de texto para tirar uma foto, escolher da
  galeria ou inserir emojis.
- Na primeira foto o app pede para baixar o **leitor de imagens**
  (SmolVLM2-500M, ~550 MB, uma única vez). Depois funciona sem internet.
- A foto é analisada pelo leitor de imagens no próprio celular e a descrição
  é entregue ao Qwen3, que responde em português. Leva de 20 a 60 segundos.

## Importante

- Identificador do app: `com.rudia.ia`. **Não mude**: o Android trata outro
  identificador como outro app, com dados separados (modelo e conversas).
- Todos os APKs são assinados com a mesma chave (segredos `RUD_KEYSTORE_*` no
  GitHub). Por isso cada versão nova instala por cima e mantém tudo.
  Guarde uma cópia da chave: sem ela, não dá para atualizar sem desinstalar.
- As conversas ficam em `files/chats/` e o modelo em `files/models/`, no
  armazenamento privado do app.
