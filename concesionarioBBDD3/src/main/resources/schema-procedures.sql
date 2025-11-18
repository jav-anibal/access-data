-- Procedimiento almacenado para contar coches por marca
-- Solo funciona en MySQL

DELIMITER //

DROP PROCEDURE IF EXISTS sp_coches_por_marca//

CREATE PROCEDURE sp_coches_por_marca()
BEGIN
SELECT
    marca,
    COUNT(*) AS total_coches,
    AVG(precio) AS precio_promedio,
    MIN(precio) AS precio_minimo,
    MAX(precio) AS precio_maximo
FROM coches
GROUP BY marca
ORDER BY total_coches DESC;
END//

DELIMITER ;