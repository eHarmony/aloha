{
  "fs": ",",
  "ifs": "|",
  "missingData": "NULL",
  "errorOnOptMissingField": false,
  "errorOnOptMissingEnum": false,
  "columns": [
    { "name": "gender", "type": "enum",    "className": "a.b.Gender", "values": [ "MALE", "FEMALE" ] },
    { "name": "weight", "type": "int",     "optional":   true },
    { "name": "likes",  "type": "string",  "vectorized": true }
  ]
}
