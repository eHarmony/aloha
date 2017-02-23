{
  "separator": ",",
  "nullValue": "NULL",
  "encoding": "hotOne",
  "imports":[],
  "features": [
    {"name": "uid",          "spec":"${id}" },
    {"name": "gender",       "spec":"${gender}",    "type": "enum", "enumClass": "com.eharmony.aloha.test.proto.Testing$GenderProto" },
    {"name": "bmi" ,         "spec":"${bmi}" },
    {"name": "aspect_ratio", "spec":"${photos[0].aspect_ratio}" }
  ]
}
