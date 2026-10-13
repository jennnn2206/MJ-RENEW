
# MJ Renew — Guía de instalación y ejecución de MinIO

## 1. ¿Qué es MinIO y para qué lo utilizamos?

MinIO es un servicio de almacenamiento de archivos compatible con Amazon S3.

En MJ Renew lo utilizamos para almacenar las fotografías que los usuarios suben desde la plataforma.

Por ejemplo:

- Fotografías iniciales de una antigüedad.
- Fotografías de avances de restauración.
- Evidencias fotográficas de los traslados.

**¿Cómo funciona?**

1. El usuario selecciona fotografías desde MJ Renew.
2. El frontend envía los archivos a Spring Boot.
3. Spring Boot utiliza `AlmacenamientoService` para comunicarse con MinIO.
4. MinIO almacena las fotografías.
5. PostgreSQL guarda las referencias relacionadas con la antigüedad.

De esta manera, las fotografías ya no tienen que introducirse manualmente mediante URLs.

---

## 2. ¿Qué necesitamos tener instalado?

Antes de empezar, asegúrate de contar con:

- **Docker Desktop:** para ejecutar MinIO.
- **IntelliJ IDEA:** para abrir y ejecutar el backend.
- **Java 21:** versión utilizada por MJ Renew.
- **PostgreSQL:** para la base de datos del proyecto.
- **Git:** para descargar y actualizar el código.

También necesitas una conexión a internet, especialmente durante la primera construcción de MinIO.

### Importante

No necesitas instalar MinIO ni Go directamente en Windows.

Tampoco necesitas crear una cuenta de MinIO, registrarte en Quay o solicitar una licencia de AIStor para ejecutar esta configuración de desarrollo.

Docker se encargará de construir y ejecutar el servidor.

---

## 3. Descargar o actualizar el proyecto

Si todavía no tienes el proyecto en tu computadora, clona el repositorio de MJ Renew desde GitHub.

Si ya tienes el proyecto, ábrelo en IntelliJ y actualiza la rama correspondiente.

La implementación de almacenamiento se encuentra en:

`feat/minio-almacenamiento`

Para consultar tu rama actual:

```powershell
git branch --show-current
```

Para obtener las actualizaciones:

```powershell
git fetch origin
```

Si ya tienes la rama creada localmente:

```powershell
git switch feat/minio-almacenamiento
git pull origin feat/minio-almacenamiento
```

Si todavía no existe localmente:

```powershell
git switch --track origin/feat/minio-almacenamiento
```

Antes de hacer `git pull`, asegúrate de no tener cambios locales sin guardar o confirmar.

### ¿Dónde ejecutamos los comandos?

En la terminal de IntelliJ, dentro de la carpeta principal del proyecto.

Ejemplo:

```text
MJ-RENEW/
├── build.gradle
├── Dockerfile.minio
├── docker-compose.yml
├── gradlew.bat
├── src/
└── docs/
```

La terminal debe estar ubicada en `MJ-RENEW`, donde se encuentra `docker-compose.yml`.

---

## 4. Abrir Docker Desktop

Primero abre Docker Desktop.

Espera hasta que Docker indique que su motor está funcionando.

Para comprobarlo, abre la terminal de IntelliJ y ejecuta:

```powershell
docker --version
```

Después:

```powershell
docker compose version
```

Si ambos comandos muestran sus versiones, Docker está disponible.

**Nota:** Docker Desktop debe permanecer funcionando mientras utilicemos MinIO.

---

## 5. Construir e iniciar MinIO

Esta es la parte principal de la instalación.

En la terminal de IntelliJ ejecuta:

```powershell
docker compose up --build -d
```

### ¿Qué hace este comando?

- Lee el archivo `docker-compose.yml`.
- Utiliza `Dockerfile.minio`.
- Descarga el entorno de Go necesario para la compilación.
- Obtiene el código fuente de MinIO desde una revisión fija de GitHub.
- Compila MinIO dentro de Docker.
- Crea e inicia el contenedor `mjrenew-minio`.
- Prepara un volumen para conservar las fotografías.

### ¿Qué esperamos ver?

Durante la primera ejecución aparecerán mensajes relacionados con la descarga de dependencias y la compilación.

Por ejemplo:

```text
go: downloading github.com/...
```

Esto es normal.

**La primera construcción puede tardar varios minutos**, dependiendo de la computadora y de la conexión a internet.

No cierres la terminal ni detengas Docker durante la construcción.

Cuando termine correctamente, el comando regresará a la terminal de PowerShell.

### Importante

No es necesario ejecutar comandos adicionales para instalar Go o descargar MinIO manualmente.

El Dockerfile realiza ese proceso.

---

## 6. Comprobar que MinIO está funcionando

Ejecuta:

```powershell
docker compose ps
```

Deberías observar algo similar:

```text
NAME            IMAGE            SERVICE   STATUS
mjrenew-minio   mj-renew-minio   minio     Up
```

El estado importante es:

`Up`

Eso significa que el contenedor está ejecutándose.

También puedes consultar sus mensajes:

```powershell
docker compose logs --tail=40 minio
```

Si todo está correcto, deberían aparecer mensajes parecidos a:

```text
MinIO Object Storage Server

API: http://127.0.0.1:9000
WebUI: http://127.0.0.1:9001
```

### ¿Qué significa cada puerto?

| Puerto | Función |
|---|---|
| 9000 | API de almacenamiento utilizada por Spring Boot. |
| 9001 | Consola web para administrar y revisar las fotografías. |

---

## 7. Abrir la consola de MinIO

Desde el navegador, abre:

http://localhost:9001

Se mostrará una pantalla de inicio de sesión.

Para desarrollo local utilizamos:

**Usuario:**

`mjrenew`

**Contraseña:**

`mjrenew123`

Estas son credenciales exclusivamente de desarrollo.

Una vez que inicies sesión, podrás acceder al Object Browser.

### ¿Qué sucede si no aparece ningún bucket?

Es normal durante la primera ejecución.

El backend de Spring Boot incluye un servicio llamado:

`AlmacenamientoService.java`

Este servicio intenta crear automáticamente el bucket cuando inicia la aplicación.

El bucket utilizado por MJ Renew se llama:

`mjrenew-fotos`

No es necesario crearlo manualmente para seguir el procedimiento normal.

---

## 8. Comprobar PostgreSQL

Antes de iniciar Spring Boot, comprueba que PostgreSQL esté funcionando.

MJ Renew utiliza una base de datos PostgreSQL local.

Cada integrante debe tener configuradas sus propias credenciales y contar con el esquema de base de datos del proyecto.

La configuración se realiza en:

`src/main/resources/application-local.properties`

Si todavía no tienes ese archivo, puedes utilizar como referencia:

`docs/implementaciones/pagos-stripe-demo/application-local.properties.example`

Copia el ejemplo a la ruta indicada y modifica las credenciales de PostgreSQL según tu computadora.

**No reemplaces el archivo si ya tienes tu configuración local funcionando.**

### Configuración de MinIO

El proyecto ya contiene estos valores por defecto en `application.properties`:

```properties
minio.endpoint=http://localhost:9000
minio.access-key=mjrenew
minio.secret-key=mjrenew123
minio.bucket=mjrenew-fotos
```

Estos valores coinciden con la configuración local de Docker.

Por lo tanto, normalmente no es necesario modificar nada para conectar Spring Boot con MinIO.

### Importante

`application-local.properties` contiene información local y no debe subirse a GitHub.

---

## 9. Iniciar Spring Boot

Una vez que MinIO y PostgreSQL estén funcionando, podemos iniciar el backend.

### Opción A: desde IntelliJ

1. Abre el proyecto MJ Renew.
2. Espera a que Gradle sincronice sus dependencias.
3. Busca la configuración `MjrenewBackendApplication`.
4. Presiona el botón verde de ejecución.
5. Espera a que Spring Boot termine de iniciar.

### Opción B: desde la terminal

En Windows ejecuta:

```powershell
.\gradlew.bat bootRun
```

### ¿Qué esperamos ver?

En la consola debería aparecer un mensaje similar a:

```text
Started MjrenewBackendApplication
```

Esto indica que el backend está funcionando.

Después abre:

http://localhost:8080

### Importante

Si Spring Boot estaba ejecutándose antes de iniciar MinIO, reinicia Spring Boot.

Esto permitirá que `AlmacenamientoService` intente inicializar nuevamente el bucket.

---

## 10. Verificar la conexión entre Spring Boot y MinIO

Con ambas aplicaciones funcionando:

1. Entra a http://localhost:9001.
2. Inicia sesión en MinIO.
3. Abre el Object Browser.
4. Actualiza la página.

Debería aparecer:

`mjrenew-fotos`

Esto significa que el bucket está disponible.

Si no aparece, revisa los mensajes de Spring Boot para identificar errores de conexión.

---

## 11. Probar la subida de fotografías desde MJ Renew

Ahora comprobaremos que el almacenamiento funcione desde la plataforma.

### Paso 1. Entrar a MJ Renew

Abre:

http://localhost:8080

Inicia sesión con una cuenta de propietario que tengas configurada en tu base de datos.

### Paso 2. Registrar una antigüedad

Desde el panel del propietario:

1. Abre la sección **Mis piezas**.
2. Selecciona la opción para registrar una nueva antigüedad.
3. Completa los datos solicitados.
4. Dirígete a la sección de fotografías iniciales.

### Paso 3. Seleccionar fotografías

En el formulario encontrarás el botón:

`Elegir archivos`

Aunque solamente aparezca un botón, permite seleccionar varias fotografías al mismo tiempo.

Para seleccionar tres imágenes en Windows:

1. Haz clic en **Elegir archivos**.
2. Mantén presionada la tecla `Ctrl`.
3. Selecciona tres fotografías.
4. Presiona **Abrir**.

También puedes seleccionar varios archivos consecutivos con `Shift`.

**Importante:** selecciona todas las fotografías en una misma operación, porque volver a abrir el selector normalmente reemplaza la selección anterior.

### Requisitos del formulario actual

- Mínimo 3 fotografías.
- Máximo 10 fotografías.
- Formatos aceptados actualmente: JPG, PNG y WEBP.
- Tamaño máximo indicado por el frontend: 5 MB por fotografía.

Para la primera prueba recomendamos utilizar tres imágenes pequeñas, menores a 1 MB cada una, porque Spring Boot también puede aplicar límites de subida.

Estos son los límites actuales de la interfaz, no necesariamente los definitivos del ERS.

### Paso 4. Registrar

Presiona:

`Registrar pieza`

Si la operación es correcta, la antigüedad debería aparecer en el panel del propietario.

---

## 12. Comprobar las fotografías en MinIO

Regresa a la consola de MinIO:

http://localhost:9001

Entra al bucket:

`mjrenew-fotos`

Busca la carpeta:

`fotos-iniciales`

Deberías observar las tres fotografías que acabas de seleccionar.

Los nombres pueden ser diferentes porque el backend genera identificadores UUID para los archivos.

Ejemplo:

```text
mjrenew-fotos/
└── fotos-iniciales/
    ├── uuid-foto1.webp
    ├── uuid-foto2.webp
    └── uuid-foto3.webp
```

Si aparecen, la subida desde Spring Boot hacia MinIO está funcionando.

---

## 13. Comprobar las fotografías desde MJ Renew

Regresa al panel del propietario.

1. Busca la antigüedad que acabas de registrar.
2. Presiona **Ver detalle**.
3. Comprueba que aparezcan sus fotografías.

Si las fotografías se muestran correctamente, habremos comprobado el flujo:

**Frontend → Spring Boot → MinIO → Visualización**

La antigüedad también debe quedar registrada en PostgreSQL.

---

## 14. Comprobar que los archivos no desaparecen

MinIO utiliza un volumen de Docker para conservar las fotografías.

Para comprobarlo, ejecuta:

```powershell
docker compose restart minio
```

Espera unos segundos y comprueba:

```powershell
docker compose ps
```

Después abre:

http://localhost:9001

Revisa que las fotografías continúen dentro del bucket.

También puedes regresar a MJ Renew y comprobar que se siguen mostrando.

**Importante:**

No utilices:

```powershell
docker compose down -v
```

La opción `-v` elimina los volúmenes asociados al proyecto y puede provocar la pérdida de las fotografías almacenadas.

---

## 15. Comandos útiles para trabajar diariamente

### Iniciar MinIO

Una vez construida la imagen:

```powershell
docker compose up -d
```

### Comprobar su estado

```powershell
docker compose ps
```

### Consultar los mensajes

```powershell
docker compose logs --tail=40 minio
```

### Reiniciar MinIO

```powershell
docker compose restart minio
```

### Detener MinIO sin borrar fotografías

```powershell
docker compose down
```

### Reconstruir MinIO cuando cambie el Dockerfile

```powershell
docker compose up --build -d
```

No es necesario reconstruir la imagen cada vez que abras el proyecto.

---

## 16. Problemas frecuentes y soluciones

### Problema 1: Docker no funciona

Si aparece un error indicando que Docker no está disponible:

- Abre Docker Desktop.
- Espera a que el motor esté funcionando.
- Vuelve a ejecutar el comando.

### Problema 2: No se encuentra docker-compose.yml

Si aparece:

```text
no configuration file provided: not found
```

Significa que probablemente estás ejecutando el comando en otra carpeta.

Entra a la carpeta `MJ-RENEW`, donde se encuentra `docker-compose.yml`, y vuelve a intentarlo.

### Problema 3: Error al descargar MinIO desde Bitnami o Quay

Si aparece:

```text
bitnami/minio:latest: not found
```

O un error `401 UNAUTHORIZED` desde Quay, comprueba que hayas actualizado la rama.

La configuración corregida utiliza `Dockerfile.minio` en lugar de depender directamente de esas imágenes.

No es necesario iniciar sesión en Quay ni pedir una licencia de AIStor.

### Problema 4: Spring Boot no encuentra AwsCredentials

Si aparece:

```text
ClassNotFoundException:
software.amazon.awssdk.auth.credentials.AwsCredentials
```

Es posible que Gradle no haya sincronizado correctamente las dependencias.

En IntelliJ, utiliza:

**Gradle → Reload All Gradle Projects**

Si continúa el problema, detén Spring Boot y ejecuta:

```powershell
.\gradlew.bat --refresh-dependencies clean bootRun
```

El proyecto utiliza el SDK de AWS S3 para comunicarse con MinIO.

### Problema 5: Spring Boot no se conecta a MinIO

Comprueba que el contenedor esté funcionando:

```powershell
docker compose ps
```

Si no está iniciado, ejecuta:

```powershell
docker compose up -d
```

Después reinicia Spring Boot.

### Problema 6: Las fotografías no se suben

Comprueba:

- Que MinIO esté ejecutándose.
- Que exista el bucket `mjrenew-fotos`.
- Que las fotografías cumplan los límites del formulario.
- Que Spring Boot no muestre errores en la consola.

Si aparece `413 Payload Too Large`, es necesario revisar los límites multipart configurados en Spring Boot.

### Problema 7: El puerto ya está ocupado

Si Docker indica que los puertos 9000 o 9001 están ocupados, revisa si ya existe otro contenedor o servicio utilizando esos puertos.

No levantes dos instancias de MinIO al mismo tiempo.

---

## 17. Consideraciones importantes para el equipo

### Cada integrante tiene su propio almacenamiento

Aunque todas utilicemos el mismo proyecto de GitHub, cada computadora tendrá su propia instancia de MinIO y su propio volumen.

Esto significa que las fotografías que una integrante suba en su computadora no aparecerán automáticamente en las computadoras de las demás.

GitHub comparte el código y la configuración, pero no sincroniza las fotografías almacenadas localmente.

### Credenciales de desarrollo

Las credenciales `mjrenew` y `mjrenew123` son únicamente para pruebas locales.

No deben utilizarse para desplegar el sistema públicamente.

### Seguridad de las fotografías

La implementación actual permite leer fotografías mediante URLs generadas por el servicio. Antes de producción deberán revisarse los permisos de acceso, las validaciones de archivos y la protección de evidencias.

### Versión de MinIO

El Dockerfile utiliza una revisión fija del código fuente de MinIO:

`7aac2a2c5b7c882e68c1ce017d8256be2feea27f`

El código se compila con Go 1.24.8 y reporta licencia GNU AGPLv3.

Es una construcción del código fuente, identificada como `DEVELOPMENT.GOGET`, no una distribución oficial de lanzamiento.

Antes de distribuirla o desplegarla públicamente, deberán revisarse sus condiciones de licencia y seguridad.

---

## 18. Resumen rápido: ¿qué hago al abrir el proyecto?

Si es la primera vez:

1. Clona o actualiza `feat/minio-almacenamiento`.
2. Abre Docker Desktop.
3. Comprueba que PostgreSQL esté funcionando.
4. Configura `application-local.properties` si todavía no lo tienes.
5. En la carpeta `MJ-RENEW`, ejecuta:

   `docker compose up --build -d`

6. Comprueba el contenedor con:

   `docker compose ps`

7. Abre http://localhost:9001.
8. Inicia Spring Boot desde IntelliJ.
9. Abre http://localhost:8080.
10. Registra una antigüedad y comprueba sus fotografías.

Si ya realizaste la instalación anteriormente:

1. Abre Docker Desktop.
2. Comprueba PostgreSQL.
3. Ejecuta `docker compose up -d`.
4. Ejecuta Spring Boot.
5. Continúa trabajando normalmente en MJ Renew.

---

## 19. Resultado esperado

Al terminar este procedimiento tendremos:

- Docker ejecutando MinIO correctamente.
- El bucket `mjrenew-fotos` disponible.
- Spring Boot conectado al almacenamiento.
- Registro de antigüedades con fotografías reales.
- Visualización de las fotografías desde el panel del propietario.
- Fotografías conservadas al reiniciar el contenedor.

Con esto, el equipo puede continuar desarrollando las funcionalidades relacionadas con imágenes sin introducir URLs manualmente.
