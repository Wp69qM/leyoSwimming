UPDATE `package_template` pt
SET pt.images = (
    SELECT JSON_ARRAYAGG(image_url)
    FROM (
        SELECT image_url
        FROM `package_template_image`
        WHERE package_template_id = pt.id
        ORDER BY sort_order
    ) sorted
)
WHERE EXISTS (
    SELECT 1 FROM `package_template_image` WHERE package_template_id = pt.id
);
