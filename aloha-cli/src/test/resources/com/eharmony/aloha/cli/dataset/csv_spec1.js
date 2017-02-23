{
  "imports":[
    "com.eharmony.aloha.feature.BasicFunctions._",
    "com.eharmony.aloha.feature.OptionMath.Syntax._",
    "scala.math._"
  ],
  "features": [
    {"name": "gender",    "spec":"ind(${gender}.toString)",      "defVal":[["=UNK",1.0]]},
    {"name": "weight",    "spec":"ind(${weight} / 10 * 10)", "defVal":[["=UNK",1.0]]},
    {"name": "num_likes", "spec":"ind(${likes}.size)",  "defVal":[["=UNK",1.0]]}
  ]
}
