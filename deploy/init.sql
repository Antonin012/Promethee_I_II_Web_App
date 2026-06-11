CREATE TABLE session (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE criterion (
    session_id VARCHAR(255) REFERENCES session(id) ON DELETE CASCADE,
    name VARCHAR(255),
    weight DOUBLE PRECISION,
    is_maximize BOOLEAN,
    function_type VARCHAR(50),
    param_p DOUBLE PRECISION,
    param_q DOUBLE PRECISION,
    param_s DOUBLE PRECISION
);

CREATE TABLE alternative (
    id VARCHAR(255) PRIMARY KEY,
    session_id VARCHAR(255) REFERENCES session(id) ON DELETE CASCADE,
    name VARCHAR(255)
);

CREATE TABLE evaluation (
    alternative_id VARCHAR(255) REFERENCES alternative(id) ON DELETE CASCADE,
    criterion_id VARCHAR(255) REFERENCES criterion(id) ON DELETE CASCADE,
    value DOUBLE PRECISION,
    PRIMARY KEY (alternative_id, criterion_id)
);
