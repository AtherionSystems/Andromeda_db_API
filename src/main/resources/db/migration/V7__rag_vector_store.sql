CREATE TABLE andromeda_vectors (
                                   id           VARCHAR2(150)  NOT NULL,
                                   entity_type  VARCHAR2(50)   NOT NULL,
                                   entity_id    NUMBER         NOT NULL,
                                   project_id   NUMBER,
                                   text_content VARCHAR2(4000) NOT NULL,
                                   embedding    VECTOR(3072, FLOAT32),
                                   created_at   TIMESTAMP DEFAULT SYSTIMESTAMP,
                                   CONSTRAINT pk_andromeda_vectors PRIMARY KEY (id)
);

CREATE INDEX idx_andromeda_vectors_project ON andromeda_vectors (project_id);
COMMIT