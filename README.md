# Reginotes API

> Sistema colaborativo de gestão de notas com suporte a workspaces, versionamento automático e edição em tempo real.

---

## 🚀 Tecnologias

- **Java 17** + **Spring Boot**
- **PostgreSQL** — banco de dados principal com Full-Text Search
- **Redis** — cache, blacklist de JWT e tokens temporários
- **RabbitMQ** — mensageria assíncrona para emails e notificações
- **MinIO** — armazenamento de arquivos e anexos
- **Docker + Docker Compose** — containerização completa
- **GitHub Actions** — CI/CD com publicação no Docker Hub
- **Swagger/OpenAPI** — documentação interativa da API

---

## ✅ Funcionalidades Implementadas

### Autenticação
- Registro e login com JWT stateless
- Access Token de curta duração com blacklist persistida no Redis
- Refresh Token de longa duração com revogação total no logout
- Recuperação de senha em 3 etapas (código 6 dígitos via email → token temporário → reset)

### Usuários
- Buscar e atualizar dados (nome, email, avatar) com campos opcionais via `Optional<T>`
- Troca de senha autenticada
- Troca de email com invalidação automática do token atual
- Desativar e deletar conta permanentemente

### Workspaces
- CRUD completo com soft delete, restore e hard delete
- Hierarquia de workspaces (parent/children)
- Sistema de convites por email com tokens temporários no Redis (TTL 3 dias)
- Hierarquia de permissões granular: `OWNER > ADMIN > EDITOR > VIEWER`
- Gerenciamento de membros: adicionar, remover e alterar cargo com validação de nível
- Notificações em tempo real via WebSocket (STOMP) + envio de email via RabbitMQ

### Notas
- CRUD completo com soft delete, restore e hard delete
- Notas vinculadas a workspaces ou independentes (órfãs)
- Hierarquia de notas (parent/children)
- Permissões herdadas do workspace ou gerenciadas via colaboradores da nota
- Versionamento automático a cada save com histórico completo
- Restore de versão com backup automático do estado atual
- Cache de leitura com Redis
- Edição colaborativa em tempo real via **Yjs + TipTap** (sincronização no frontend, persistência via REST)
- Conteúdo armazenado como JSONB (estrutura gerenciada pelo TipTap/ProseMirror)

### Colaboradores de Nota
- Adicionar e remover colaboradores com controle de permissões (`OWNER > EDITOR > VIEWER`)
- Owner inserido automaticamente via trigger no banco ao criar nota
- Validação de hierarquia: só altera/remove colaboradores abaixo do próprio nível

### Tags
- CRUD de tags vinculadas a workspaces
- Atribuição e remoção de tags em notas
- Permissão de criação/edição restrita a EDITOR+

### Notificações
- Persistência no banco + entrega em tempo real via WebSocket
- Tipos: convite de workspace, nota compartilhada, nota atualizada, colaborador adicionado, versão restaurada
- Envio de email assíncrono via RabbitMQ

---

## 🏗️ Arquitetura

```
Frontend (Next.js + Yjs/TipTap)
        ↓ REST + WebSocket (STOMP)
Spring Boot API
        ↓
┌───────────────────────────────────┐
│  PostgreSQL  │  Redis  │  MinIO   │
│  RabbitMQ    │                    │
└───────────────────────────────────┘
```

**Camadas:** Controller → Service → Repository  
**Segurança:** Spring Security + JWT stateless  
**Migrations:** Flyway (versionadas, imutáveis)  
**Mensageria:** RabbitMQ com duas filas independentes (email, indexação)  
**Cache:** Redis com invalidação explícita por chave

---

## 🗄️ Banco de Dados

### ENUMs
- `workspace_role`: `OWNER`, `ADMIN`, `EDITOR`, `VIEWER`
- `note_role`: `OWNER`, `EDITOR`, `VIEWER`
- `notification_type`: `WORKSPACE_INVITE`, `NOTE_SHARED`, `NOTE_UPDATED`, `COLLABORATOR_JOINED`, `VERSION_RESTORED`

### Tabelas principais
`app_users`, `workspaces`, `workspace_members`, `refresh_tokens`, `notes`, `note_collaborators`, `note_versions`, `note_tags`, `tags`, `attachments`, `notifications`

### Decisões técnicas
- UUIDs como PK
- Soft delete com `deleted_at` em notas e workspaces
- JSONB para conteúdo de notas (gerenciado pelo frontend)
- Triggers para versionamento automático e inserção do owner como colaborador
- `Instant` para todas as datas

---

## 🔒 Segurança

- JWT com blacklist no Redis (TTL = tempo restante do token)
- WebSocket com autenticação no STOMP CONNECT via `ChannelInterceptor`
- Sessão STATELESS
- Hierarquia de permissões validada em todas as operações de escrita

---

## 🐳 Infraestrutura

```bash
# Subir o ambiente completo
docker compose up -d

# A aplicação estará disponível em:
# API:           http://localhost:8080
# Swagger:       http://localhost:8080/swagger-ui.html
# RabbitMQ UI:   http://localhost:15672
```

> O arquivo `.env` nunca é versionado. Crie o seu localmente com as variáveis necessárias.

---

## 🗺️ Roadmap

- [ ] AttachmentService com MinIO
- [ ] Busca Full-Text com PostgreSQL FTS
- [ ] ExportService (PDF, Markdown, HTML) — Strategy Pattern
- [ ] Validação de SUBSCRIBE no WebSocket por nota
- [ ] OAuth2 (Google, GitHub)
- [ ] Rate limiting nos endpoints de autenticação
- [ ] Testes unitários e de integração

---

## 📄 Documentação da API

A documentação interativa está disponível via Swagger em `/swagger-ui.html` após subir a aplicação.

Uma documentação voltada para desenvolvedores frontend está disponível no repositório como `Reginotes - Documentação da API.docx`.