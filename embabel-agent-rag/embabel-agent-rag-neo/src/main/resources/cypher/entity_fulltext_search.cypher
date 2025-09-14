CALL db.index.fulltext.queryNodes($fulltextIndex, $searchText)
YIELD node AS m, score
WHERE score >= $similarityThreshold
RETURN m AS match, m.name as name, m.description as description, m.id AS id, labels(m) AS labels,
       score
ORDER BY score DESC