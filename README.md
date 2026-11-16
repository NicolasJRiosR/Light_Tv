# Light TV

Aplicación Android desarrollada en **Kotlin** y **Jetpack Compose** para controlar dispositivos compatibles mediante el emisor infrarrojo (IR) integrado en el teléfono.

Actualmente, el proyecto está enfocado en el control de una lámpara RGB mediante un perfil de códigos NEC extendido. La idea es mantener una base sencilla y ampliable para incorporar otros dispositivos infrarrojos en el futuro.

> **Estado:** el control RGB está implementado. El control para televisores Heisen/VIDAA todavía no está integrado en la aplicación.

## Funcionalidades actuales

- Detección del emisor infrarrojo del teléfono.
- Consulta de los rangos de frecuencia que informa Android.
- Envío de comandos infrarrojos a **38 kHz**.
- Encendido y apagado de la lámpara.
- Selección de colores disponibles en el perfil.
- Ajuste de brillo, cuando el comando está definido.
- Efectos Flash, Strobe, Fade y Smooth, cuando están definidos.
- Interfaz construida con Jetpack Compose y tema oscuro.

## Compatibilidad

La aplicación necesita un teléfono Android que tenga un emisor IR compatible con la API `ConsumerIrManager`.

- **Android mínimo configurado:** Android 6.0 (API 23).
- **API utilizada para IR:** `ConsumerIrManager` (disponible desde Android 4.4).
- **Frecuencia del perfil actual:** 38.000 Hz (38 kHz).
- **Protocolo del perfil actual:** NEC extendido.
- **Dirección del perfil:** `0xEF00`.

Tener Android o Bluetooth no garantiza que el teléfono pueda transmitir infrarrojos. El dispositivo debe incorporar físicamente un emisor IR.

## Perfil de lámpara

El proyecto conserva un perfil candidato:

| Propiedad | Valor |
|---|---|
| Nombre | RGB + blanco · NEC extendido · 21 teclas |
| Identificador | `rgb21-nec-ext` |
| Protocolo | NEC extendido |
| Dirección | `0xEF00` |
| Frecuencia portadora | 38 kHz |

El perfil define los comandos de encendido/apagado, brillo, colores y efectos. Los códigos proceden de una tabla comunitaria y deben considerarse **candidatos**: otros modelos de lámpara pueden utilizar direcciones, comandos o protocolos diferentes.

## Cómo funciona el infrarrojo

1. La aplicación comprueba si Android detecta un emisor IR.
2. La interfaz habilita los controles cuando el emisor está disponible.
3. Al pulsar un botón, se consulta el código asociado en el perfil.
4. `NecEncoder` construye la secuencia de pulsos del protocolo NEC.
5. `IrTransmitter` envía la secuencia mediante `ConsumerIrManager`.

La aplicación puede confirmar que Android aceptó el envío, pero **no puede confirmar que la lámpara recibió o ejecutó el comando**.

## Estructura del proyecto

```text
src/main/
├── AndroidManifest.xml
└── java/com/example/rgbcontroller/
    ├── MainActivity.kt
    ├── domain/
    │   └── RgbRemotePreset.kt
    └── hardware/
        ├── IrTransmitter.kt
        └── NecEncoder.kt
```

### Componentes principales

- **`MainActivity.kt`**: pantalla principal, controles de la lámpara y estado de la aplicación.
- **`domain/RgbRemotePreset.kt`**: acciones disponibles y perfil de códigos infrarrojos.
- **`hardware/IrTransmitter.kt`**: detección del emisor IR y transmisión de señales.
- **`hardware/NecEncoder.kt`**: codificación de direcciones y comandos en una trama NEC.

## Tecnologías

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Android `ConsumerIrManager`
- Gradle Kotlin DSL

Configuración del proyecto consultada:
- `compileSdk = 36`
- `minSdk = 23`
- `targetSdk = 35`
- Java 17
- Kotlin Compose plugin 2.2.10

## Abrir y ejecutar

1. Clona el repositorio:

   ```bash
   git clone https://github.com/NicolasJRiosR/Light_Tv.git
   ```

2. Abre el proyecto en Android Studio.
3. Espera a que Gradle sincronice las dependencias.
4. Conecta un teléfono Android compatible con emisor infrarrojo.
5. Ejecuta la configuración de la aplicación desde Android Studio.

También puedes actualizar una copia local existente con:

```bash
git pull origin main
```

## Permisos y características Android

El manifiesto declara:

- `android.permission.TRANSMIT_IR`: permiso para transmitir señales infrarrojas.
- `android.hardware.consumerir`: característica de hardware IR marcada como no obligatoria para permitir instalar la app en dispositivos sin emisor; la aplicación comprueba su disponibilidad antes de habilitar el control.

## Limitaciones conocidas

- El perfil actual está pensado para una familia concreta de lámparas RGB; no garantiza compatibilidad universal.
- No hay aprendizaje ni recepción de señales IR: el teléfono transmite, pero no captura los códigos del mando original.
- No se confirma la recepción del comando por parte del dispositivo controlado.
- Bluetooth no forma parte del control de luces implementado actualmente; el uso de Bluetooth para reproducir música en una lámpara es independiente del envío IR.
- **El control de televisores Heisen con VIDAA aún no está desarrollado.** Que un perfil Hisense funcione desde otra aplicación de control remoto es una pista para investigar, pero sus códigos deben verificarse antes de incorporarlos a Light TV.

## Próximas mejoras posibles

- Añadir perfiles para otros modelos de lámparas.
- Incorporar un apartado independiente para televisores y otros equipos IR.
- Permitir seleccionar y guardar perfiles de dispositivos.
- Añadir pruebas de compatibilidad de comandos.

## Repositorio

[ N i c o l a s J R i o s R / Light_Tv ](https://github.com/NicolasJRiosR/Light_Tv)
