import bem from "bem-ts"
import React from "react"
import "./IconSection.less"

const b = bem("iconsection")

export type IconSectionProps = {
  icon: React.ReactNode
  children: React.ReactNode
}

export const IconSection = (props: IconSectionProps) => (
  <section className={b()}>
    <div className={b("icon")}>{props.icon}</div>
    <div className={b("content")}>{props.children}</div>
  </section>
)