import { update } from "ramda"
import React, { useState } from "react"
import { Checkbox } from "../forms/Checkbox"
import { DataTable, Props as DataTableProps } from "./DataTable"

export type Props = DataTableProps & {
  onChange: (selectedKeys: string[]) => void
}

export const SelectableDataTable = ({ data, onChange, ...rest }: Props) => {
  const [selectedKeys, setSelectedKeys] = useState<string[]>([])

  const dataWithCheckboxes = data.map((datum) => ({
    ...datum,
    values: datum.values[0]
      ? update(
          0,
          {
            ...datum.values[0],
            icon: (
              <Checkbox
                value={selectedKeys.includes(datum.key)}
                onChange={(selected) => {
                  const newKeys = selected
                    ? [...selectedKeys, datum.key]
                    : selectedKeys.filter((key) => key !== datum.key)
                  setSelectedKeys(newKeys)
                  onChange(newKeys)
                }}
              />
            ),
          },
          datum.values
        )
      : datum.values,
  }))
  return <DataTable data={dataWithCheckboxes} {...rest} />
}
