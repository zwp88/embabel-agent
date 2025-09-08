CALL db.index.fulltext.queryNodes($fulltextIndex, $searchText)
YIELD node AS chunk, score
  WHERE score >= $similarityThreshold
RETURN chunk.text AS text, chunk.id AS id,
       score
  ORDER BY score DESC