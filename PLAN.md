# Plan de la app móvil de clientes de PlusContable

Repo: `plus-contable-cliente-app` (KMP, **solo Android** por ahora). Backend: el repo `plus-contable` (su plan: `docs/plan-app-cliente.md`).
Entrega: **mié 18/11/2026 ~12:00**. Es un proyecto aparte del Sprint 2 y no entra en su backlog ni en su burndown.

## Qué es

Un portal de consulta para el cliente MYPE del estudio. Tiene cinco pantallas: ver qué debe y cuándo, ver qué le cobra el estudio, enviar una
incidencia y descargar sus documentos. **No** muestra ni edita datos de la clave SOL ni datos tributarios.

## Arquitectura

```
App KMP  ──HTTP + JWT──►  ms-auth (login)  /  ms-tributario (datos, filtrados por clienteId del token)
```

No hay backend propio ni acceso a la base de datos. La app solo habla con la API de PlusContable.

## Tecnologías

| Capa | Elección |
|---|---|
| Lenguaje y UI | Kotlin + **Compose Multiplatform** (módulo `shared`) y `androidApp` como cáscara |
| Red | **Ktor Client** + `kotlinx.serialization` (DTO a mano, según `docs/contrato-api-cliente.md` del backend) |
| Navegación | Navigation Compose |
| Inyección | Koin |
| Sesión | JWT en almacenamiento cifrado de Android (`multiplatform-settings` sobre `EncryptedSharedPreferences`) |
| Pruebas | `kotlin.test` + Ktor `MockEngine` |
| Compilación | Gradle con catálogo de versiones; `minSdk` 26 |

Versiones: fijarlas al crear el proyecto, mirando las actuales de Kotlin y Compose Multiplatform.

**Se descartan** por el plazo: iOS, SQLDelight (caché offline), biometría, pasarela de pago, push (FCM) y subir comprobantes.

## Historias (cada una remite a una ruta de la API)

| HU | Historia | Pts | Ruta de la API | Tareas |
|---|---|---|---|---|
| **B** | Inicio sesión y la app me recuerda | 3 | `POST /api/auth/login`, `GET /api/auth/me` | 1. Proyecto KMP. 2. Cliente Ktor con `Authorization`. 3. Pantalla de login. 4. Guardar el token. 5. Cerrar sesión local |
| **C** | Veo mi semáforo y mis obligaciones | 5 | `GET /calendario?clienteId=`, `GET /obligaciones?clienteId=` | 1. Inicio (estado y próximo vencimiento). 2. Lista con estado. 3. Carga, error y vacío |
| **D** | Veo mis honorarios y cuotas | 3 | `GET /clientes/{id}/cobros`, `GET /clientes/{id}/fraccionamientos` | 1. Lista de cobros. 2. Detalle con cuotas |
| **E** | Reporto un problema o consulta | 3 | `POST /incidencias`, `GET /clientes/{id}/incidencias` | 1. Formulario. 2. Historial de mis incidencias |
| **F** | Veo mis avisos | 2 | `GET /clientes/{id}/notificaciones` | 1. Lista de avisos de SUNAT (solo lectura) |
| **G** | Descargo mis documentos | 3 | `GET /clientes/{id}/documentos` y `/{doc}/archivo` | 1. Lista. 2. Descarga y apertura del PDF |

Total: 19 puntos (los 5 de HU-A son del backend).
El `clienteId` sale del JWT; la app nunca lo pide al usuario.

## Calendario

| Día | Meta |
|---|---|
| Lun 9 | Repo clonado, `git init` + remoto, `.gitignore`, esqueleto KMP corriendo en emulador; contrato del backend recibido |
| Mar 10 | HU-B con datos de prueba (`MockEngine`) |
| Mié 11 | HU-B contra la API real (requiere cuenta de cliente: HU-A1) |
| Jue 12 – Vie 13 | HU-C y HU-D |
| Sáb 14 | HU-E |
| Dom 15 | Colchón |
| Lun 16 | HU-F y HU-G |
| Mar 17 | Prueba en un celular real, APK firmado, guion de demo |
| Mié 18 mañana | Correcciones y entrega ~12:00 |

## Reglas del repo

- `main` protegida; ramas `feature/...` y PR, como en PlusContable.
- **No subir:** keystore de firma, `local.properties`, `google-services.json` y cualquier URL o credencial de entornos reales.
- La URL base del backend va por tipo de compilación (`debug` y `release`). Emulador: `http://10.0.2.2:<puerto>`. Celular real: la IP de la PC o un túnel HTTPS; HTTP simple solo se permite en `debug`.
- Repo privado, con los compañeros y el docente como colaboradores.

## Riesgos

- **Sin cuenta de cliente real no hay login:** depende de HU-A1 del backend (siembra o HU-37). Mientras tanto, `MockEngine`.
- **Primera compilación de KMP:** configurar Gradle y el SDK puede costar medio día; por eso el esqueleto va el primer día.
- **Contrato cambiante:** si el JSON cambia, hay que retocar los DTO a mano (no se genera código).
- **Si algo se retrasa**, se recorta en este orden: HU-G, HU-F, HU-E.
