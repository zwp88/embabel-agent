MERGE (e:ContentElement {id: $id})
SET e += $properties
WITH e
CALL apoc.create.addLabels(e, $labels)
YIELD node
RETURN 1