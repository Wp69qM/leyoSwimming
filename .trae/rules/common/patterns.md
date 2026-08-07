# Common Patterns

## Skeleton Projects

When implementing new functionality:
1. Search for battle-tested skeleton projects
2. Use parallel agents to evaluate options:
   - Security assessment
   - Extensibility analysis
   - Relevance scoring
   - Implementation planning
3. Clone best match as foundation
4. Iterate within proven structure

## Design Patterns

### Repository Pattern

Encapsulate data access behind a consistent interface:
- Define standard operations: findAll, findById, create, update, delete
- Concrete implementations handle storage details (database, API, file, etc.)
- Business logic depends on the abstract interface, not the storage mechanism
- Enables easy swapping of data sources and simplifies testing with mocks

### API Response Format

Use a consistent envelope for all API responses:
- Include a success/status indicator
- Include the data payload (nullable on error)
- Include an error message field (nullable on success)
- Include metadata for paginated responses (total, page, limit)

### API URL Pattern (RPC over HTTP)

All backend HTTP APIs MUST follow [api-convention.md](../../../docs/tech/api-convention.md):
- Use `POST` for all endpoints (list, detail, create, update, delete, business actions)
- URL structure: `/api/{module}/{singular-resource}/{action}`
- Action names: `list`, `detail`, `add`, `update`, `delete`, or the business verb (e.g., `approve`, `reject`, `cancel`)
- Pass IDs, filters, and pagination parameters in the JSON body, not in the URL path
- Avoid mixing GET/POST on the same resource path; each action gets its own unique URL
