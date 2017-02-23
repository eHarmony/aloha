{
  "separator": ",",
  "nullValue": "NULL",
  "encoding": "regular",
  "imports":[
    "com.eharmony.aloha.feature.BasicFunctions._"
  ],
  "features": [
    {"name": "csv_label",        "spec":"2 * (${id} % 2) - 1" },
    {"name": "gender",           "spec":"${gender}.ordinal"   },
    {"name": "bmi",              "spec":"${bmi}"              },
    {"name": "num_photos",       "spec":"${photos}.size"      },
    {"name": "avg_photo_height", "spec":"{ val hs = ${photos.height};  hs.flatten.sum / hs.filter(_.nonEmpty).size }" }
  ],
  "namespaces": [
    { "name": "ignored", "features": [ "csv_label" ] },
    { "name": "personal",  "features": [ "gender", "bmi" ] },
    { "name": "photos",  "features": [ "num_photos", "avg_photo_height" ] }
  ],
  "label": "2 * (${id} % 2) - 1"
}
