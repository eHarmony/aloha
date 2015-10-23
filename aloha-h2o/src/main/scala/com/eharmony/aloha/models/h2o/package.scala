package com.eharmony.aloha.models

import hex.genmodel.easy.RowData

/**
 * Created by deak on 10/13/15.
 */
package object h2o {
  private[h2o] implicit class RowDataOps(val rowData: RowData) extends AnyVal {
    @inline def +(k: String, v: AnyRef): RowData = {
      rowData.put(k, v)
      rowData
    }
  }
}
