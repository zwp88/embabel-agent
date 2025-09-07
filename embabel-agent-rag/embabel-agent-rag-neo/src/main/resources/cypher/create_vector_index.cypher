CREATE VECTOR INDEX `embabel-content-index` IF NOT EXISTS
FOR (n:ContentElement) ON (n.embedding)
OPTIONS {indexConfig: {
`vector.dimensions`: $dimensions,
`vector.similarity_function`: 'cosine'
}}
