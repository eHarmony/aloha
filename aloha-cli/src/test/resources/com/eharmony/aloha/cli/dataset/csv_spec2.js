{
  "separator": ",",
  "nullValue": "NULL",
  "encoding": "regular",
  "imports":[],
  "features": [
    {"name": "gender",    "spec":"${gender}" },
    {"name": "weight",    "spec":"${weight} / 10 * 10" },
    {"name": "num_likes", "spec":"${likes}.size" }
  ]
}
