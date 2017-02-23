{
  "notes": [
    "Notice the gender field has additional fields: 'type' and 'values'.  This is required for non-'regular'",
    "encodings.  Non-'regular' encodings need to use the typed API to exploit the information about categorical",
    "variables when creating the encoded field.",

    "Note additionally, that this version of enumerated is not backed by a real java enumerated type but rather by",
    "a synthetic one (com.eharmony.aloha.semantics.compiled.plugin.csv.EnumConstant) since the associated input type",
    "in the unit test comes from CSV input.  The output type of the spec needs to be a String, but EnumConstant",
    "exposes an implicit conversion to String.  The output of the spec needs to result in one of the strings ",
    "{'MALE', 'FEMALE'}.  This is guaranteed by the input.",

    "There is a second type of enum specification that IS backed by a real java enumerated type.  To use this type",
    "of enum, provide the 'enumClass' field with a String containing the canonical classpath to the enum."
  ],

  "separator": ",",
  "nullValue": "NULL",
  "encoding": "hotOne",
  "imports":[],
  "features": [
    {"name": "gender",    "spec":"${gender}",    "type": "enum", "values": [ "MALE", "FEMALE" ] },
    {"name": "weight" ,   "spec":"${weight} / 10 * 10" },
    {"name": "num_likes", "spec":"${likes}.size" }
  ]
}
