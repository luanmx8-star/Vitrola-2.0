# Correção da seleção de pastas — Vitrola

O botão **Adicionar pasta** no Android agora abre o seletor de pastas do sistema (`ACTION_OPEN_DOCUMENT_TREE`). O plugin Android percorre a pasta selecionada e suas subpastas, lista até 5.000 arquivos de áudio e entrega os URIs SAF ao WebView pelo bridge local do Capacitor. O JavaScript importa cada áudio para a biblioteca existente e preserva o caminho relativo das subpastas. No navegador, continua usando o seletor web como alternativa.

A correção não pede permissão ampla de armazenamento: o acesso é concedido pelo usuário à pasta escolhida. O Android pode impedir a escolha de algumas pastas protegidas pelo sistema, como áreas privadas de outros apps.

## Teste no celular

O arquivo `vitrola-corrigida-teste.apk` é uma variante assinada para teste com identificador `com.vitrola.player.corrigido` e nome **Vitrola Corrigida**. Ele pode ser instalado ao lado do app original sem removê-lo. Como é um app separado, a biblioteca local não é compartilhada: selecione/importa as músicas novamente nessa versão para testar.

## Atualização do app original

A correção foi compilada como APK de depuração. A assinatura desse build não corresponde à assinatura do APK originalmente enviado. Portanto, ele não pode substituir a instalação original mantendo os dados. Para publicar uma atualização sobre o app atual, compile este projeto com a chave de assinatura original; o APK não contém a chave privada. O arquivo de teste com identificador separado evita a necessidade de desinstalar o app existente.

## Build

Com Node.js 22+, Java 21 e Android SDK API 36:

```bash
npm ci
npm run android:sync
cd android
./gradlew assembleDebug
```

O build deste ambiente foi validado com `assembleDebug` e a sintaxe do JavaScript embutido foi verificada com `node --check`. A seleção e a importação ainda precisam ser testadas em um aparelho Android real, pois não há dispositivo Android conectado a este ambiente.
