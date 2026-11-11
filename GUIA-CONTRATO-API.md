# Guía: escribir `docs/contrato-api-cliente.md` (en el repo del BACKEND)

Para una sesión de Claude Code abierta en el repo `plus-contable` (`D:\Disco D\Proyectos Claude\PlusContable`), no en la app.
Tarea A5 de `docs/plan-app-cliente.md`. La escribe Jeanpierre y se la entrega a Julio (`fabianrodriguez33`), que lleva el backend.

## Objetivo

Un documento que diga, ruta por ruta, qué JSON devuelve el backend hoy a un cliente `CLIENTE_MYPE`. La app móvil escribe sus DTO a mano a partir de él.
**Se documenta lo que el código hace, no lo que se desearía.** Lo que no exista se marca como propuesta.

## Reglas (no negociables)

- **No cambiar código del backend.** Solo se crea/edita documentación. Julio es dueño del backend y podría pisar la misma rama.
- Trabajar en la rama `feature/api-cliente` (existe, con el plan). No empezar desde otra rama con cambios sin commit: revisar `git status` primero.
- **No hacer `git push`** sin que Jeanpierre lo confirme.
- No poner URLs de entornos reales, contraseñas ni tokens reales en los ejemplos. Los datos de ejemplo van inventados.
- Escribir en español.

## Rutas a documentar (las que usa la app)

| # | Ruta | Pantalla de la app | Estado (según el plan) |
|---|---|---|---|
| 1 | `POST /api/auth/login` | Login | Existe, pública |
| 2 | `GET /api/auth/me` | Sesión | Existe |
| 3 | `POST /api/auth/logout` | Cerrar sesión | **El plan dice que no existe**; la app lo llama e ignora el error. Confirmar |
| 4 | `GET /api/tributario/obligaciones?clienteId=` y `/{id}` | Obligaciones | Abierta a `CLIENTE_MYPE` |
| 5 | `GET /api/tributario/calendario?clienteId=` | Semáforo | Solo estudio; se abrirá (A2) |
| 6 | `GET /api/tributario/clientes/{id}/cobros` | Cobros | Solo estudio; se abrirá (A2) |
| 7 | `GET /api/tributario/clientes/{id}/fraccionamientos` | Cuotas | Solo estudio; se abrirá (A2) |
| 8 | `POST /api/tributario/incidencias`, `GET .../clientes/{id}/incidencias` | Incidencias | Solo estudio; se abrirá (A2) |
| 9 | `GET /api/tributario/clientes/{id}/notificaciones` | Avisos | **No existe**: documentar como propuesta (A3) |
| 10 | `GET .../clientes/{id}/documentos` y `/{doc}/archivo` | Documentos | Abierta |

Los prefijos exactos (`/api/tributario/...`) hay que **verificarlos en los controladores**; los de la tabla son los supuestos del plan.
Prioridad si falta tiempo: 1, 2, 4, 5, 6, 7 primero (alcanzan para login + semáforo + obligaciones + honorarios). Luego 8, 10 y 9.

## Cómo averiguar cada ruta (en este orden)

1. Buscar el controlador: `@GetMapping`/`@PostMapping` en `ms-auth` y `ms-tributario` (`Grep` por la ruta).
2. Leer el DTO o entidad que devuelve (clases `*Dto`, `*Response`, o la entidad si se devuelve directo). Anotar **nombre exacto de cada campo y su tipo Java**.
3. Mirar cómo se serializan fechas y dinero: `LocalDate`, `LocalDateTime`, `BigDecimal`, `Instant`. Revisar `application*.yml` y configuración de Jackson (`write-dates-as-timestamps`, formato). Esto decide si llega `"2026-11-13"`, `[2026,11,13]` o un número.
4. Mirar los enums: copiar **todos los valores posibles** (por ejemplo los estados de obligación y de cobro).
5. Revisar `SecurityConfig` y `docs/HU-38-matriz-de-permisos.md`: qué roles pasan hoy y qué responde cuando el `clienteId` es ajeno (403; Beto aún no decide si será 404).
6. Revisar el manejo de errores (`@ControllerAdvice`/`GlobalExceptionHandler`): forma exacta del JSON de error. La app espera `{"mensaje": "..."}`; si es otra, anotarlo.
7. Si existen pruebas del controlador, copiar de ahí un JSON real de ejemplo.
8. Si algo no se puede determinar leyendo el código, **no inventarlo**: escribir `VERIFICAR:` y qué falta (por ejemplo, correr el servicio con Docker y llamar la ruta).

## Plantilla del documento

```markdown
# Contrato de la API para la app de clientes

Versión: borrador · Fecha: AAAA-MM-DD · Autor: <nombre>

## Reglas generales
- URL base: `<variable por entorno; no poner la real>`. Emulador: `http://10.0.2.2:<puerto>`.
- Autenticación: `Authorization: Bearer <token>` en todas las rutas salvo login.
- Token: <tiempo de vida, claims — incluir `clienteId`>. Confirmar si `/me` devuelve `clienteId`.
- Fechas: <formato real>. Dinero: <tipo real: número o texto, decimales>.
- Error: `<JSON real>` con los códigos 401 / 403 / 404 / 400 que existan.
- El `clienteId` sale del token; si se pide el de otro cliente: <403 o 404>.

## <MÉTODO> <ruta>
**Pantalla:** <cuál>  **Quién puede:** <roles>  **Estado:** existe / se abre en A2 / propuesta
**Parámetros:** <query/path/body con tipo y si es obligatorio>
**Respuesta 200:**
```json
{ "ejemplo": "real o inventado pero con los campos exactos" }
```
| Campo | Tipo | Notas (valores del enum, formato, puede ser null) |
|---|---|---|
**Errores:** <código — cuándo>
```

## Lo que la app asume HOY (comparar y avisar de cada diferencia)

DTO provisionales de la app (`shared/.../datos/`). Si el backend usa otros nombres o tipos, **anotar la diferencia al final del contrato** en una sección «Diferencias con la app», para que se ajuste la app y no el backend:

```kotlin
Login:       LoginRequest(username, password)
             LoginResponse(token, tipo, expiraEnSegundos, usuario: Usuario(id, username, nombreCompleto?, roles[]))
Obligacion:  id: Long, tributo, periodo "AAAA-MM", vence "AAAA-MM-DD", estado ("PENDIENTE" | "PRESENTADA" | "VENCIDA")
Cobro:       id: Long, concepto, periodo, monto: Double, vence "AAAA-MM-DD", estado ("PENDIENTE" | "PAGADO")
Cuota:       numero: Int, vence "AAAA-MM-DD", monto: Double, estado
Error:       { "mensaje": String }
```

Además la app necesita: el `clienteId` (de `/me` o del JWT), y un modo de saber el **semáforo** (hoy lo calcula la app con las obligaciones; si `/calendario` ya lo manda, documentarlo).

## Entrega

1. Guardar en `docs/contrato-api-cliente.md` y actualizar la matriz `docs/HU-38-matriz-de-permisos.md` solo si cambia algo (A5 lo pide).
2. Commit en `feature/api-cliente`, mensaje corto en español, **sin** línea `Co-Authored-By` de IA (preferencia del equipo).
3. No hacer push. Avisar a Jeanpierre con: lista de rutas documentadas, lista de `VERIFICAR:` pendientes y lista de diferencias con la app.
4. Jeanpierre se lo envía a Julio; la app ajusta sus DTO y escribe `DatosApi`.

## Hecho cuando

- Cada ruta de la tabla tiene método, ruta real, roles, JSON de ejemplo y errores, o está marcada `VERIFICAR:` / `propuesta`.
- Los formatos de fecha, dinero y error están escritos, no supuestos.
- La app puede escribir sus DTO sin abrir el backend.
