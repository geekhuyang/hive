/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.ddl.view;

import org.apache.hadoop.hive.ql.ddl.DDLOperationContext;
import org.apache.hadoop.hive.ql.ddl.DDLUtils;
import org.apache.hadoop.hive.ql.ddl.DDLOperation;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.HiveMaterializedViewsRegistry;
import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.ql.metadata.Table;

/**
 * Operation process of dropping a materialized view.
 */
public class DropMaterializedViewOperation extends DDLOperation<DropMaterializedViewDesc> {
  public DropMaterializedViewOperation(DDLOperationContext context, DropMaterializedViewDesc desc) {
    super(context, desc);
  }

  @Override
  public int execute() throws HiveException {
    Table table = getTable();
    if (table == null) {
      return 0; // dropping not existing materialized view is handled by DDLSemanticAnalyzer
    }

    if (!table.isMaterializedView()) {
      if (desc.isIfExists()) {
        return 0;
      } else if (table.isView()) {
        throw new HiveException("Cannot drop a view with DROP MATERIALIZED VIEW");
      } else {
        throw new HiveException("Cannot drop a base table with DROP MATERIALIZED VIEW");
      }
    }

    // TODO: API w/catalog name
    context.getDb().dropTable(desc.getTableName(), false);
    HiveMaterializedViewsRegistry.get().dropMaterializedView(table.getDbName(), table.getTableName());
    DDLUtils.addIfAbsentByName(new WriteEntity(table, WriteEntity.WriteType.DDL_NO_LOCK), context);

    return 0;
  }

  private Table getTable() throws HiveException {
    try {
      return context.getDb().getTable(desc.getTableName());
    } catch (InvalidTableException e) {
      return null;
    }
  }
}
