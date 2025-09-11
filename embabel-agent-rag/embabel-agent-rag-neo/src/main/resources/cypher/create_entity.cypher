MATCH (chunk:$($chunkNodeName) {id: $basisId})
CREATE (e:$($entityLabels) {id: $id, name: $name, description: $description, createdDate: timestamp()})
<-[:HAS_ENTITY]-(chunk)
SET e += $properties,
 e.lastModifiedDate = timestamp()
RETURN COUNT(e) as nodesCreated