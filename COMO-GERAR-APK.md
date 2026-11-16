# Como gerar o APK do Vitrola

Este projeto inclui o app web original, um wrapper Android Capacitor e um workflow do GitHub Actions. O workflow gera um **APK de depuração** para instalar e testar em um celular Android. Ele não publica o app na Play Store.

## Enviar os arquivos ao GitHub

1. Extraia o ZIP no computador ou aparelho. Crie um repositório no GitHub pela sua conta.
2. Envie para a raiz da branch `main` o conteúdo da pasta `vitrola` extraída — não envie apenas o ZIP fechado. Inclua as pastas ocultas `.github/` e `android/`.
3. Abra a aba **Actions** do repositório. O workflow **Build Android APK** roda quando houver um push relevante na branch `main`; também pode ser iniciado em **Actions → Build Android APK → Run workflow**.
4. Quando a execução terminar com sucesso, abra a execução e baixe **vitrola-debug-apk** na seção **Artifacts**. Extraia o ZIP baixado para obter `app-debug.apk`.
5. Transfira o APK ao Android e autorize a instalação do arquivo para testar o app.

O workflow usa Node.js 22, Java 21 e Android SDK nos runners hospedados pelo GitHub. Não é necessário adicionar tokens ou secrets para esse build.

## Abrir ou compilar localmente

Instale Node.js 22 ou superior, Android Studio 2025.2.1 ou superior, Android SDK Platform 36 e Build Tools 36.0.0. O app mantém suporte mínimo à API 24. Na pasta do projeto, execute:

```bash
npm ci
npm run android:sync
npm run android:open
```

No Android Studio, abra a pasta `android/`, escolha um dispositivo/emulador e use **Run**. Para compilar por terminal:

```bash
cd android
./gradlew assembleDebug
```

O arquivo resultante fica em `android/app/build/outputs/apk/debug/app-debug.apk`.

## Identidade do app e assinatura

O identificador inicial deste app é `com.vitrola.player` e o nome exibido é `Vitrola`. Escolha o identificador definitivo antes de distribuir atualizações para usuários, pois mudar o application ID cria outro app para o Android.

O APK de depuração é assinado com uma chave de teste criada pelo Android Gradle Plugin. Ele serve para teste e instalação manual. Para distribuir na Play Store, será necessário configurar uma chave de assinatura de release e um workflow de release separado; essas credenciais não devem ser colocadas nos arquivos do repositório.

## Controles de mídia

A página usa a Web Media Session quando o WebView/Android fornece suporte. O wrapper e o workflow, por si só, não adicionam o plugin Android `VitrolaMedia` que publica uma notificação nativa e mantém reprodução em segundo plano. Essa integração nativa é uma etapa separada; a disponibilidade dos controles do sistema pode variar até ela ser implementada e validada no aparelho.

## Referências oficiais

- [Instalar Capacitor em um app web existente](https://capacitorjs.com/docs/getting-started)
- [Requisitos de ambiente do Capacitor](https://capacitorjs.com/docs/getting-started/environment-setup)
- [Plataforma Android do Capacitor](https://capacitorjs.com/docs/android)
- [GitHub Actions com Gradle e artefatos](https://docs.github.com/en/actions/use-cases-and-examples/building-and-testing/building-and-testing-java-with-gradle)
