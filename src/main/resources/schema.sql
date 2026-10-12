CREATE TABLE IF NOT EXISTS t_user
(
    id        BIGINT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    age       INT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
