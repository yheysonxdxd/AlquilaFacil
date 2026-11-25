# Hospedaje Supply (AlquilaFácil)

Programa de escritorio para Windows que maneja **dos negocios en uno**:

- **Alquiler mensual de cuartos:** inquilinos, luz y agua, quién debe y cuánto, recibos en PDF.
- **Hospedaje por noches:** reservas, llegadas, salidas, adelantos, calendario de ocupación y comprobantes por WhatsApp.

Hecho con **Spring Boot 3 + JavaFX 21 + JPA/Hibernate + SQLite + AtlantaFX**, con patrón MVC.

---

## Pantallas

| Menú | Qué hace |
|---|---|
| **Inicio** | Lo cobrado del mes, la deuda vencida, quiénes están atrasados, la ocupación y el gráfico de ingresos. Desde aquí se cobra o se envía el recordatorio por WhatsApp. Avisa los **contratos que vencen en 30 días** (con "Renovar 1 año"). Tiene el **Reporte del mes** y el **Reporte anual para el contador** (PDF y Excel; solo administrador). |
| **Caja del día** | Lo que entró y salió en el día, separado por forma de pago (Efectivo, Yape, Plin, Transferencia) y por usuario. Tiene "contar la caja" para ver si cuadra, sobra o falta, **Guardar cierre** (queda registrado quién cerró, cuánto contó y la nota si sobró o faltó) e **Imprimir cierre** en PDF con firmas. |
| **Gastos y ganancia** *(solo administrador)* | Gastos del negocio y la ganancia real del mes, de cada negocio. |
| **Cuartos** | Cuartos de alquiler mensual: precio, libre u ocupado y por quién. |
| **Inquilinos** | Datos, cuarto, **renta pactada**, fecha de ingreso, día de pago, garantía y **fin del contrato**. Botón **Contrato PDF** para imprimir y firmar. La salida genera una liquidación en PDF que descuenta o devuelve la garantía. |
| **Pagos y recibos** | Cobro de alquiler, luz y agua con recibo PDF numerado y monto en letras. Historial con la columna **COBRÓ**. Para anular un recibo hay que escribir el **motivo**. |
| **Habitaciones** | Habitaciones de hospedaje con fotos, comodidades, normas y estado de limpieza. Si el cuarto tiene **foto 360°**, la foto grande de la ficha **es el 360°**: gira sola y se arrastra con el mouse para mirar todo el cuarto (rueda = acercar; botón de arriba = pantalla completa; las miniaturas cambian a las fotos normales). Si no tiene 360°, la foto tiene **efecto 3D** al mover el mouse. Pasa en las fichas de hospedaje y de cuartos mensuales. |
| **Estadías y reservas** | Registro de huéspedes, adelanto y saldo, **editar** una estadía (datos, fechas, habitación o precio), llegada (check-in), "no vino", salida normal o anticipada (con devolución), calendario de 14 días y WhatsApp con el comprobante. |

## Exportar a Excel

Todo se descarga como **Excel de verdad (.xlsx)**: títulos en negrita, montos como números (se pueden sumar), fechas como fechas, filtros en cada columna y la cabecera fija al bajar.

| Dónde | Qué descarga |
|---|---|
| **Pagos y recibos → Excel** | Todos los recibos (o los de la búsqueda), con estado, quién cobró y motivo de anulación. |
| **Estadías y reservas → Excel** | Las estadías que se ven en la tabla (respeta el filtro y la búsqueda): fechas, total, pagado, saldo, devuelto, estado. |
| **Inquilinos → Excel** | Inquilinos activos (o también retirados) con renta, día de pago, garantía y contrato. |
| **Gastos y ganancia → Excel** | Los gastos del mes elegido. |
| **Caja del día → Excel** | Movimientos, resumen por forma de pago y anulaciones del día. |
| **Historial de cambios → Excel** | Lo que se ve en el historial. |
| **Inicio → Reporte del mes / anual → Excel** | Mes: 5 hojas (resumen, alquiler, hospedaje, gastos, ocupación). Año: 12 meses con total, listo para el contador. |

## Menú lateral (administrador)

- **Usuarios**, **Diseño y colores**, **Copia de seguridad**.
- **Datos del negocio:** nombre, DNI, dirección, celular, hospedaje, hora límite de llegada, tasa del impuesto y **minutos sin uso para cerrar la sesión** (15 por defecto; 0 = nunca).
- **Historial de cambios:** quién borró, editó, anuló o canceló algo, y cuándo, con los valores de antes y después. No se puede modificar.

## Usuarios y permisos

- La primera vez el programa pide crear el **administrador** y le entrega un **código de recuperación** (ej. `K7QM-3XRP-W9TD`). Anótalo en papel.
- **¿El administrador olvidó su contraseña?** En la pantalla de ingreso: "¿Olvidaste tu contraseña?", usuario + código → contraseña nueva. El código se gasta y se entrega uno nuevo. Tras 5 intentos fallidos se bloquea 15 minutos. El código se crea o cambia en **Usuarios → Crear/Cambiar mi código de recuperación**.
- Si alguien de **recepción** olvida su contraseña, el administrador le pone una nueva desde **Usuarios**.
- **Administrador:** puede todo, incluido anular, cancelar, borrar, ver gastos y ganancia, administrar usuarios y restaurar copias.
- **Recepción:** registra inquilinos, cobros, estadías, llegadas y salidas. En "Caja del día" solo ve lo que cobró esa misma persona. No ve gastos ni cambia precios.
- Cada cobro, anulación y cancelación guarda **quién la hizo, cuándo y por qué**, y todo lo delicado queda en el **Historial de cambios**.
- Si nadie usa el programa durante los minutos configurados, **la sesión se cierra sola**.
- Las contraseñas se guardan cifradas (PBKDF2), nunca en texto.

## Copias de seguridad

- Se hace **una copia automática cada día** al abrir el programa, en la carpeta `respaldos`. Se guardan las últimas 30.
- El botón **Copia de seguridad** permite:
  - hacer una copia en ese momento,
  - **volver a una copia anterior**,
  - restaurar desde otro archivo, por ejemplo una USB o Drive.
- Antes de restaurar, el programa guarda una copia de lo que había (`..._antes-de-restaurar.db`).
- **Copia fuera de esta PC:** en "Copia de seguridad" eliges una carpeta de **Google Drive** (ej. `G:\Mi unidad`) o una **USB**. Desde entonces, cada copia también se guarda allí, en la subcarpeta `Hospedaje Supply - respaldos`. Si un día no llega (USB desconectada), se reintenta al abrir el programa. Si pasan 7 días sin que llegue ninguna, el programa avisa al administrador.
- En `respaldos\antiguos` están las copias manuales que se hicieron antes de que existiera esta función.

## Archivos de la carpeta del programa

| Archivo / carpeta | Qué es |
|---|---|
| `alquileres.db` | **La base de datos** (todos los datos). No la borres. |
| `configuracion.properties` | Configuración **privada** de esta PC, como el token de RENIEC. No la compartas. |
| `preferencias.properties` | El diseño de colores elegido. |
| `respaldos\` | Copias de seguridad. |
| `fotos\` | Fotos de las habitaciones. |
| `comprobantes\` | Comprobantes PDF enviados por WhatsApp. |
| `logs\` | **Registro de errores y actividad** (`hospedaje.log`, uno por día, se guardan 30 días). Si el programa no abre, el detalle queda en `logs\error-arranque.txt`. Envía esta carpeta a quien te dé soporte cuando algo falle. |
| `empaquetado\` | El programa ya armado (`Hospedaje Supply.exe`). |

### Token de RENIEC (búsqueda por DNI)

El token **no** va dentro del programa. Va en `configuracion.properties`, en la carpeta del programa:

```
app.dni.token=sk_...
```

Si cambias de token, solo edita esa línea y vuelve a abrir el programa.

### Datos del negocio (contratos, recibos y mensajes)

Se cambian **desde el programa** en el menú lateral **Datos del negocio** (solo administrador): nombre, DNI, dirección, celular, nombre del hospedaje, hora límite de llegada y tasa del impuesto. Se guardan en la base (tabla `ajuste`), así que también viajan en las copias de seguridad, y no hace falta reempaquetar. Mientras sigan los datos de ejemplo, el programa lo avisa al ingresar como administrador.

El **reporte anual** muestra un estimado *referencial* del impuesto del alquiler mensual con la tasa que pongas en **Datos del negocio** (5 % por defecto). El monto real y la forma de declarar los confirma tu contador.

### Cobros con Yape / Plin (QR)

En **Datos del negocio → Cobros con Yape / Plin** pones tu celular de Yape/Plin, a nombre de quién está y la **imagen de tu QR** (en Yape: Menú → Mi QR → descargar o captura). Desde entonces:

- El **comprobante de hospedaje** trae el QR al lado de la firma, con lo que falta pagar.
- Los **mensajes de WhatsApp** (confirmación con saldo, recordatorio de llegada y recordatorio de pago a inquilinos) dicen "Puede pagar por Yape / Plin al ...".
- En los recordatorios a inquilinos, el **QR queda copiado**: en el chat de WhatsApp presiona Ctrl+V para adjuntarlo.

La imagen se guarda como `qr-pago.png` en la carpeta del programa.

Los **datos de ejemplo** están apagados (`app.seeder.enabled=false`). Si la base se pierde, el programa empieza vacío y no carga datos falsos.

## Reglas del negocio

**Alquiler mensual**
- Cada inquilino tiene un **día de pago**. Si el mes es más corto (por ejemplo, día 31 en febrero), vence el último día.
- Un mes está **pendiente** desde el mes de ingreso hasta el actual si no tiene un pago válido. Si ya pasó el día de pago, está **atrasado**.
- Se puede cobrar un mes por adelantado, pero no el mismo mes dos veces.
- Cada inquilino tiene su **renta pactada**: si después cambias el precio del cuarto, lo que ya debe no cambia. Los inquilinos registrados antes de esta función recibieron como renta el precio que tenía su cuarto ese día.
- **El mes en que entra y el mes en que se va se cobran por días.** Ejemplo: renta 350 y entra el 25 de un mes de 30 días: paga 350 × 6 ÷ 30 = S/ 70. Se cuentan el día de entrada y el de salida.
- Un cuarto solo puede tener un inquilino activo.
- El contrato puede tener fecha de fin (opcional). 30 días antes aparece en Inicio. "Renovar 1 año" suma 12 meses desde la fecha de fin, o desde hoy si ya venció hace más de un mes.
- Anular un recibo no lo borra: queda en el historial con su motivo y el mes vuelve a figurar pendiente.

**Hospedaje**
- **Reserva de grupo (familia):** en "Nueva estadía", con **Agregar otra habitación**, una sola persona alquila varias habitaciones en las mismas fechas y paga todo. Lo que paga ahora se reparte entre las habitaciones en orden. Sale **un solo comprobante** con todas las habitaciones, "Cobrar saldo" cobra lo que falta de todo el grupo de una vez y "Llegó" registra la llegada de todas. Cada habitación se puede editar, cancelar o dar de salida por separado.
- **Consumos extra:** desayuno, lavandería, bebidas, cochera, etc. Se cargan con el botón **Consumo** o desde la ficha de la estadía ("Agregar consumo"). Se pagan en el momento o quedan **a la cuenta**; "Cobrar consumos" cobra todo lo pendiente (de todo el grupo si es reserva de grupo). Salen en el comprobante, en la caja del día y en lo cobrado del hospedaje. No se puede registrar la salida ni cancelar con consumos sin pagar. Anular un consumo: solo el administrador, con motivo. Al escribir un consumo ya usado, se pone solo el precio de la última vez.
- No se puede reservar una habitación en fechas que se cruzan con otra reserva.
- Máximo 60 noches por estadía y no más personas que la capacidad de la habitación.
- Si el huésped ya llegó y debe saldo, no se puede cancelar ni registrar su salida sin cobrar.
- Salida anticipada: se cobran solo las noches usadas y se registra la devolución, o el cobro de lo que falta.
- **Devoluciones** (salida anticipada o "no vino"): el programa pregunta **cómo** devuelves el dinero (efectivo, Yape, Plin o transferencia). Queda anotado en la estadía (monto, forma, fecha y quién), en la **Caja del día** como salida de esa forma de pago y en el **Historial de cambios**. El comprobante trae la línea de devolución y un espacio para que el huésped firme "Recibí conforme".
- **Editar una estadía:** si ya terminó o ya tuvo salida anticipada, solo se corrigen los datos del huésped. El nuevo total no puede quedar debajo de lo ya pagado (para devolver dinero está la salida anticipada). Si el saldo ya se cobró, no se puede subir el total (registra una estadía nueva). Recepción no puede cambiar el precio por noche.
- "No vino": la habitación se libera. El adelanto se queda como penalidad, salvo que se marque devolverlo.

## Cómo trabajar con el código

### Abrir en IntelliJ IDEA
1. Requisitos: **JDK 17 o superior**. El empaquetado usa `C:\Program Files\Java\jdk-22`.
2. **File → Open** → carpeta `alquila-facil`. IntelliJ detecta el `pom.xml`.
3. Ejecuta `src/main/java/com/alquilafacil/YCLauncher.java`. Usa siempre **YCLauncher**, no `YCJavaFxApplication`.

### Pruebas automáticas
```
.\mvnw.cmd test
```
Son 100 pruebas, incluida una **prueba de pantallas** (`PantallasTest`): arranca el programa completo con datos de ejemplo en una carpeta temporal (sin tocar tu base), abre las 8 pantallas y las ventanas principales, y falla si alguna lanza un error. Cubren cobros y anulaciones, hospedaje (reservas cruzadas, saldo, cancelación, "no vino", salida anticipada), caja del día, contratos (vencimiento, renovación y PDF), reporte anual, datos del negocio, código de recuperación, renta pactada y cobro por días, edición de estadías, cierre de caja, copia externa, historial, permisos, copias de seguridad, cálculo de vencimientos, monto en letras y contraseñas.

### Crear / actualizar el .exe
Cierra el programa y, desde esta carpeta:
```
powershell -ExecutionPolicy Bypass -File empaquetar.ps1
```
Esto crea `empaquetado\programa\Hospedaje Supply\Hospedaje Supply.exe` y el acceso directo en el escritorio. El .exe usa los datos de **esta** carpeta (`-Dapp.carpeta`).

### Historial de cambios (git)
El proyecto está en git. Los datos (`*.db`, fotos, respaldos, comprobantes) y la configuración privada **no** se guardan en git.
```
git log --oneline          # ver los cambios guardados
git status                 # ver que cambio desde el ultimo guardado
git add -A
git commit -m "Que cambie"  # guardar una version
git restore <archivo>      # deshacer cambios no guardados de un archivo
```

## Estructura (MVC)

```
com.alquilafacil
├── model/        Entidades JPA: Cuarto, Inquilino, Pago, Estadia, Gasto, Liquidacion, Usuario...
├── repository/   Consultas (Spring Data JPA)
├── service/      Reglas del negocio: pagos, estadias, caja, respaldos, PDFs, usuarios, sesion
├── controller/   Pantallas JavaFX. Las grandes se dividen en piezas:
│                 Hospedaje → AccionesEstadia, DetalleEstadia, MensajesHuesped,
│                             CalendarioOcupacion, EstadoEstadia
│                 Habitaciones → FichaHabitacion
├── ui/           Componentes comunes: Notificador, Tablas, Formularios, Exportador, PantallaIngreso...
├── seeder/       Datos de ejemplo (apagado)
└── config/       Datos del arrendador
resources/
├── view/*.fxml   Diseño de las pantallas
└── css/app.css   Estilos y temas de color
```
