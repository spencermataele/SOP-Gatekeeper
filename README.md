First feature plan for MVP:
  Backend
    Sop entity, repository, service, controller (/api/sops).
    Basic CRUD.
    Flyway V1 (tables) is already created.
  Frontend
    features/sops/ with list + create/edit form.
    SopService hits /api/sops.
    Minimal Material table + form.
  Stretch: simple change-log on sop_attribute edits.
