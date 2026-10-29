# Rud IA

Assistente de IA que roda 100% no celular (Android), usando o modelo local
**Qwen3-4B Q4_K_M (GGUF, ~2,5 GB)** com llama.cpp. Sem respostas simuladas.

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
- Se já tiver o arquivo `Qwen3-4B-Q4_K_M.gguf` no celular, use **Importar .gguf**.
- Depois disso, o modelo carrega automaticamente toda vez que o app abre.

## Importante

- Identificador do app: `com.rudia.ia`. **Não mude**: o Android trata outro
  identificador como outro app, com dados separados (modelo e conversas).
- Todos os APKs são assinados com a mesma chave (segredos `RUD_KEYSTORE_*` no
  GitHub). Por isso cada versão nova instala por cima e mantém tudo.
  Guarde uma cópia da chave: sem ela, não dá para atualizar sem desinstalar.
- As conversas ficam em `files/chats/` e o modelo em `files/models/`, no
  armazenamento privado do app.
