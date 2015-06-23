{
  "imports":[
    "com.eharmony.matching.aloha.feature.BasicFunctions._",
    "com.eharmony.matching.aloha.feature.OptionMath.Syntax._",
    "scala.math._"
  ],
  "features": [
    {"name": "name",       "spec":"ind(${name})",   "defVal":[["=UNK",1.0]]},
    {"name": "gender",     "spec":"ind(${gender})", "defVal":[["=UNK",1.0]]},
    {"name": "bmi",        "spec":"${bmi}",         "defVal":[["=UNK",1.0]]},
    {"name": "num_photos", "spec":"${photos}.size" }
  ]
}
