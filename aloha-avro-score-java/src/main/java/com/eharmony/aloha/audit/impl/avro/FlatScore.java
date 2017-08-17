/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.eharmony.aloha.audit.impl.avro;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** Information in here mirrors the information in Score and FlatScoreDescendant.  That means
   that when treating a FlatScore or Score as a GenericRecord, the information for the top-level
   score can be accessed uniformly. */
@org.apache.avro.specific.AvroGenerated
public class FlatScore extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -8637973361660658603L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"FlatScore\",\"namespace\":\"com.eharmony.aloha.audit.impl.avro\",\"doc\":\"Information in here mirrors the information in Score and FlatScoreDescendant.  That means\\n   that when treating a FlatScore or Score as a GenericRecord, the information for the top-level\\n   score can be accessed uniformly.\",\"fields\":[{\"name\":\"model\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"ModelId\",\"fields\":[{\"name\":\"id\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"default\":null}]}],\"default\":null},{\"name\":\"value\",\"type\":[\"null\",\"boolean\",\"int\",\"long\",\"float\",\"double\",\"string\",{\"type\":\"array\",\"items\":[\"boolean\",\"int\",\"long\",\"float\",\"double\",\"string\"]}],\"default\":null},{\"name\":\"subvalueIndices\",\"type\":[{\"type\":\"array\",\"items\":\"int\"},\"null\"],\"doc\":\"Each value in subvalues is an index in the FlatScore.descendants.\\n     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a\\n     subvalue of this FlatScore.\",\"default\":[]},{\"name\":\"errorMsgs\",\"type\":[{\"type\":\"array\",\"items\":\"string\"},\"null\"],\"default\":[]},{\"name\":\"missingVarNames\",\"type\":[{\"type\":\"array\",\"items\":\"string\"},\"null\"],\"default\":[]},{\"name\":\"prob\",\"type\":[\"null\",\"float\"],\"default\":null},{\"name\":\"descendants\",\"type\":[{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"FlatScoreDescendant\",\"fields\":[{\"name\":\"model\",\"type\":[\"null\",\"ModelId\"],\"default\":null},{\"name\":\"value\",\"type\":[\"null\",\"boolean\",\"int\",\"long\",\"float\",\"double\",\"string\",{\"type\":\"array\",\"items\":[\"boolean\",\"int\",\"long\",\"float\",\"double\",\"string\"]}],\"default\":null},{\"name\":\"subvalueIndices\",\"type\":[{\"type\":\"array\",\"items\":\"int\"},\"null\"],\"doc\":\"Each value in subvalues is an index in the FlatScore.descendants.\\n     E.g., 1 means the FlatScoreDescendant at index 1 in FlatScore.descendants is a\\n     subvalue of this FlatScoreDescendant.\",\"default\":[]},{\"name\":\"errorMsgs\",\"type\":[{\"type\":\"array\",\"items\":\"string\"},\"null\"],\"default\":[]},{\"name\":\"missingVarNames\",\"type\":[{\"type\":\"array\",\"items\":\"string\"},\"null\"],\"default\":[]},{\"name\":\"prob\",\"type\":[\"null\",\"float\"],\"default\":null}]}},\"null\"],\"default\":[]}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public com.eharmony.aloha.audit.impl.avro.ModelId model;
  @Deprecated public java.lang.Object value;
  /** Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore. */
  @Deprecated public java.util.List<java.lang.Integer> subvalueIndices;
  @Deprecated public java.util.List<java.lang.CharSequence> errorMsgs;
  @Deprecated public java.util.List<java.lang.CharSequence> missingVarNames;
  @Deprecated public java.lang.Float prob;
  @Deprecated public java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> descendants;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public FlatScore() {}

  /**
   * All-args constructor.
   * @param model The new value for model
   * @param value The new value for value
   * @param subvalueIndices Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
   * @param errorMsgs The new value for errorMsgs
   * @param missingVarNames The new value for missingVarNames
   * @param prob The new value for prob
   * @param descendants The new value for descendants
   */
  public FlatScore(com.eharmony.aloha.audit.impl.avro.ModelId model, java.lang.Object value, java.util.List<java.lang.Integer> subvalueIndices, java.util.List<java.lang.CharSequence> errorMsgs, java.util.List<java.lang.CharSequence> missingVarNames, java.lang.Float prob, java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> descendants) {
    this.model = model;
    this.value = value;
    this.subvalueIndices = subvalueIndices;
    this.errorMsgs = errorMsgs;
    this.missingVarNames = missingVarNames;
    this.prob = prob;
    this.descendants = descendants;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return model;
    case 1: return value;
    case 2: return subvalueIndices;
    case 3: return errorMsgs;
    case 4: return missingVarNames;
    case 5: return prob;
    case 6: return descendants;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: model = (com.eharmony.aloha.audit.impl.avro.ModelId)value$; break;
    case 1: value = (java.lang.Object)value$; break;
    case 2: subvalueIndices = (java.util.List<java.lang.Integer>)value$; break;
    case 3: errorMsgs = (java.util.List<java.lang.CharSequence>)value$; break;
    case 4: missingVarNames = (java.util.List<java.lang.CharSequence>)value$; break;
    case 5: prob = (java.lang.Float)value$; break;
    case 6: descendants = (java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'model' field.
   * @return The value of the 'model' field.
   */
  public com.eharmony.aloha.audit.impl.avro.ModelId getModel() {
    return model;
  }

  /**
   * Sets the value of the 'model' field.
   * @param value the value to set.
   */
  public void setModel(com.eharmony.aloha.audit.impl.avro.ModelId value) {
    this.model = value;
  }

  /**
   * Gets the value of the 'value' field.
   * @return The value of the 'value' field.
   */
  public java.lang.Object getValue() {
    return value;
  }

  /**
   * Sets the value of the 'value' field.
   * @param value the value to set.
   */
  public void setValue(java.lang.Object value) {
    this.value = value;
  }

  /**
   * Gets the value of the 'subvalueIndices' field.
   * @return Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
   */
  public java.util.List<java.lang.Integer> getSubvalueIndices() {
    return subvalueIndices;
  }

  /**
   * Sets the value of the 'subvalueIndices' field.
   * Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
   * @param value the value to set.
   */
  public void setSubvalueIndices(java.util.List<java.lang.Integer> value) {
    this.subvalueIndices = value;
  }

  /**
   * Gets the value of the 'errorMsgs' field.
   * @return The value of the 'errorMsgs' field.
   */
  public java.util.List<java.lang.CharSequence> getErrorMsgs() {
    return errorMsgs;
  }

  /**
   * Sets the value of the 'errorMsgs' field.
   * @param value the value to set.
   */
  public void setErrorMsgs(java.util.List<java.lang.CharSequence> value) {
    this.errorMsgs = value;
  }

  /**
   * Gets the value of the 'missingVarNames' field.
   * @return The value of the 'missingVarNames' field.
   */
  public java.util.List<java.lang.CharSequence> getMissingVarNames() {
    return missingVarNames;
  }

  /**
   * Sets the value of the 'missingVarNames' field.
   * @param value the value to set.
   */
  public void setMissingVarNames(java.util.List<java.lang.CharSequence> value) {
    this.missingVarNames = value;
  }

  /**
   * Gets the value of the 'prob' field.
   * @return The value of the 'prob' field.
   */
  public java.lang.Float getProb() {
    return prob;
  }

  /**
   * Sets the value of the 'prob' field.
   * @param value the value to set.
   */
  public void setProb(java.lang.Float value) {
    this.prob = value;
  }

  /**
   * Gets the value of the 'descendants' field.
   * @return The value of the 'descendants' field.
   */
  public java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> getDescendants() {
    return descendants;
  }

  /**
   * Sets the value of the 'descendants' field.
   * @param value the value to set.
   */
  public void setDescendants(java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> value) {
    this.descendants = value;
  }

  /**
   * Creates a new FlatScore RecordBuilder.
   * @return A new FlatScore RecordBuilder
   */
  public static com.eharmony.aloha.audit.impl.avro.FlatScore.Builder newBuilder() {
    return new com.eharmony.aloha.audit.impl.avro.FlatScore.Builder();
  }

  /**
   * Creates a new FlatScore RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new FlatScore RecordBuilder
   */
  public static com.eharmony.aloha.audit.impl.avro.FlatScore.Builder newBuilder(com.eharmony.aloha.audit.impl.avro.FlatScore.Builder other) {
    return new com.eharmony.aloha.audit.impl.avro.FlatScore.Builder(other);
  }

  /**
   * Creates a new FlatScore RecordBuilder by copying an existing FlatScore instance.
   * @param other The existing instance to copy.
   * @return A new FlatScore RecordBuilder
   */
  public static com.eharmony.aloha.audit.impl.avro.FlatScore.Builder newBuilder(com.eharmony.aloha.audit.impl.avro.FlatScore other) {
    return new com.eharmony.aloha.audit.impl.avro.FlatScore.Builder(other);
  }

  /**
   * RecordBuilder for FlatScore instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<FlatScore>
    implements org.apache.avro.data.RecordBuilder<FlatScore> {

    private com.eharmony.aloha.audit.impl.avro.ModelId model;
    private com.eharmony.aloha.audit.impl.avro.ModelId.Builder modelBuilder;
    private java.lang.Object value;
    /** Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore. */
    private java.util.List<java.lang.Integer> subvalueIndices;
    private java.util.List<java.lang.CharSequence> errorMsgs;
    private java.util.List<java.lang.CharSequence> missingVarNames;
    private java.lang.Float prob;
    private java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> descendants;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.eharmony.aloha.audit.impl.avro.FlatScore.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.model)) {
        this.model = data().deepCopy(fields()[0].schema(), other.model);
        fieldSetFlags()[0] = true;
      }
      if (other.hasModelBuilder()) {
        this.modelBuilder = com.eharmony.aloha.audit.impl.avro.ModelId.newBuilder(other.getModelBuilder());
      }
      if (isValidValue(fields()[1], other.value)) {
        this.value = data().deepCopy(fields()[1].schema(), other.value);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.subvalueIndices)) {
        this.subvalueIndices = data().deepCopy(fields()[2].schema(), other.subvalueIndices);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.errorMsgs)) {
        this.errorMsgs = data().deepCopy(fields()[3].schema(), other.errorMsgs);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.missingVarNames)) {
        this.missingVarNames = data().deepCopy(fields()[4].schema(), other.missingVarNames);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.prob)) {
        this.prob = data().deepCopy(fields()[5].schema(), other.prob);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.descendants)) {
        this.descendants = data().deepCopy(fields()[6].schema(), other.descendants);
        fieldSetFlags()[6] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing FlatScore instance
     * @param other The existing instance to copy.
     */
    private Builder(com.eharmony.aloha.audit.impl.avro.FlatScore other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.model)) {
        this.model = data().deepCopy(fields()[0].schema(), other.model);
        fieldSetFlags()[0] = true;
      }
      this.modelBuilder = null;
      if (isValidValue(fields()[1], other.value)) {
        this.value = data().deepCopy(fields()[1].schema(), other.value);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.subvalueIndices)) {
        this.subvalueIndices = data().deepCopy(fields()[2].schema(), other.subvalueIndices);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.errorMsgs)) {
        this.errorMsgs = data().deepCopy(fields()[3].schema(), other.errorMsgs);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.missingVarNames)) {
        this.missingVarNames = data().deepCopy(fields()[4].schema(), other.missingVarNames);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.prob)) {
        this.prob = data().deepCopy(fields()[5].schema(), other.prob);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.descendants)) {
        this.descendants = data().deepCopy(fields()[6].schema(), other.descendants);
        fieldSetFlags()[6] = true;
      }
    }

    /**
      * Gets the value of the 'model' field.
      * @return The value.
      */
    public com.eharmony.aloha.audit.impl.avro.ModelId getModel() {
      return model;
    }

    /**
      * Sets the value of the 'model' field.
      * @param value The value of 'model'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setModel(com.eharmony.aloha.audit.impl.avro.ModelId value) {
      validate(fields()[0], value);
      this.modelBuilder = null;
      this.model = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'model' field has been set.
      * @return True if the 'model' field has been set, false otherwise.
      */
    public boolean hasModel() {
      return fieldSetFlags()[0];
    }

    /**
     * Gets the Builder instance for the 'model' field and creates one if it doesn't exist yet.
     * @return This builder.
     */
    public com.eharmony.aloha.audit.impl.avro.ModelId.Builder getModelBuilder() {
      if (modelBuilder == null) {
        if (hasModel()) {
          setModelBuilder(com.eharmony.aloha.audit.impl.avro.ModelId.newBuilder(model));
        } else {
          setModelBuilder(com.eharmony.aloha.audit.impl.avro.ModelId.newBuilder());
        }
      }
      return modelBuilder;
    }

    /**
     * Sets the Builder instance for the 'model' field
     * @param value The builder instance that must be set.
     * @return This builder.
     */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setModelBuilder(com.eharmony.aloha.audit.impl.avro.ModelId.Builder value) {
      clearModel();
      modelBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'model' field has an active Builder instance
     * @return True if the 'model' field has an active Builder instance
     */
    public boolean hasModelBuilder() {
      return modelBuilder != null;
    }

    /**
      * Clears the value of the 'model' field.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearModel() {
      model = null;
      modelBuilder = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'value' field.
      * @return The value.
      */
    public java.lang.Object getValue() {
      return value;
    }

    /**
      * Sets the value of the 'value' field.
      * @param value The value of 'value'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setValue(java.lang.Object value) {
      validate(fields()[1], value);
      this.value = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'value' field has been set.
      * @return True if the 'value' field has been set, false otherwise.
      */
    public boolean hasValue() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'value' field.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearValue() {
      value = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'subvalueIndices' field.
      * Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
      * @return The value.
      */
    public java.util.List<java.lang.Integer> getSubvalueIndices() {
      return subvalueIndices;
    }

    /**
      * Sets the value of the 'subvalueIndices' field.
      * Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
      * @param value The value of 'subvalueIndices'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setSubvalueIndices(java.util.List<java.lang.Integer> value) {
      validate(fields()[2], value);
      this.subvalueIndices = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'subvalueIndices' field has been set.
      * Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
      * @return True if the 'subvalueIndices' field has been set, false otherwise.
      */
    public boolean hasSubvalueIndices() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'subvalueIndices' field.
      * Each value in subvalues is an index in the FlatScore.descendants.
     E.g., 0 means the FlatScoreDescendant at index 0 in FlatScore.descendants is a
     subvalue of this FlatScore.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearSubvalueIndices() {
      subvalueIndices = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'errorMsgs' field.
      * @return The value.
      */
    public java.util.List<java.lang.CharSequence> getErrorMsgs() {
      return errorMsgs;
    }

    /**
      * Sets the value of the 'errorMsgs' field.
      * @param value The value of 'errorMsgs'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setErrorMsgs(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[3], value);
      this.errorMsgs = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'errorMsgs' field has been set.
      * @return True if the 'errorMsgs' field has been set, false otherwise.
      */
    public boolean hasErrorMsgs() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'errorMsgs' field.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearErrorMsgs() {
      errorMsgs = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'missingVarNames' field.
      * @return The value.
      */
    public java.util.List<java.lang.CharSequence> getMissingVarNames() {
      return missingVarNames;
    }

    /**
      * Sets the value of the 'missingVarNames' field.
      * @param value The value of 'missingVarNames'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setMissingVarNames(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[4], value);
      this.missingVarNames = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'missingVarNames' field has been set.
      * @return True if the 'missingVarNames' field has been set, false otherwise.
      */
    public boolean hasMissingVarNames() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'missingVarNames' field.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearMissingVarNames() {
      missingVarNames = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'prob' field.
      * @return The value.
      */
    public java.lang.Float getProb() {
      return prob;
    }

    /**
      * Sets the value of the 'prob' field.
      * @param value The value of 'prob'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setProb(java.lang.Float value) {
      validate(fields()[5], value);
      this.prob = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'prob' field has been set.
      * @return True if the 'prob' field has been set, false otherwise.
      */
    public boolean hasProb() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'prob' field.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearProb() {
      prob = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'descendants' field.
      * @return The value.
      */
    public java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> getDescendants() {
      return descendants;
    }

    /**
      * Sets the value of the 'descendants' field.
      * @param value The value of 'descendants'.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder setDescendants(java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant> value) {
      validate(fields()[6], value);
      this.descendants = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'descendants' field has been set.
      * @return True if the 'descendants' field has been set, false otherwise.
      */
    public boolean hasDescendants() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'descendants' field.
      * @return This builder.
      */
    public com.eharmony.aloha.audit.impl.avro.FlatScore.Builder clearDescendants() {
      descendants = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    @Override
    public FlatScore build() {
      try {
        FlatScore record = new FlatScore();
        if (modelBuilder != null) {
          record.model = this.modelBuilder.build();
        } else {
          record.model = fieldSetFlags()[0] ? this.model : (com.eharmony.aloha.audit.impl.avro.ModelId) defaultValue(fields()[0]);
        }
        record.value = fieldSetFlags()[1] ? this.value : (java.lang.Object) defaultValue(fields()[1]);
        record.subvalueIndices = fieldSetFlags()[2] ? this.subvalueIndices : (java.util.List<java.lang.Integer>) defaultValue(fields()[2]);
        record.errorMsgs = fieldSetFlags()[3] ? this.errorMsgs : (java.util.List<java.lang.CharSequence>) defaultValue(fields()[3]);
        record.missingVarNames = fieldSetFlags()[4] ? this.missingVarNames : (java.util.List<java.lang.CharSequence>) defaultValue(fields()[4]);
        record.prob = fieldSetFlags()[5] ? this.prob : (java.lang.Float) defaultValue(fields()[5]);
        record.descendants = fieldSetFlags()[6] ? this.descendants : (java.util.List<com.eharmony.aloha.audit.impl.avro.FlatScoreDescendant>) defaultValue(fields()[6]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}