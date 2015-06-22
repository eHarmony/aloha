{
  "fs": ",",
  "ifs": "|",
  "missingData": "NULL",
  "errorOnOptMissingField": false,
  "errorOnOptMissingEnum": false,
  "columns": {
    "gender": { "type": "enum",   "className": "a.b.Gender", "values": [ "MALE", "FEMALE" ] },
    "weight": { "type": "int",    "optional": true },
    "likes":  { "type": "string", "vectorized": true }
  }
}
