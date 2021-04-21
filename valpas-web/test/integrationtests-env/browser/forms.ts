import { Key } from "selenium-webdriver"
import { attributeEventuallyEquals } from "./content"
import { $ } from "./core"
import { driver } from "./driver"
import { eventually } from "./utils"

export const setTextInput = async (selector: string, value: string) => {
  await clearTextInput(selector)

  const element = await $(selector)
  await element.sendKeys(value, Key.ENTER)

  await attributeEventuallyEquals(selector, "value", value)
}

export const getTextInput = async (selector: string) => {
  const element = await $(selector)
  return element.getAttribute("value")
}

// https://stackoverflow.com/questions/53698075/how-to-clear-text-input
export const clearTextInput = async (selector: string, timeout = 1000) =>
  // Pitää tehdä silmukassa, koska tämä ei aina toimi, välillä BACK_SPACE poistaa vain viimeisen merkin
  eventually(async () => {
    const element = await $(selector)
    await driver.executeScript((element: any) => element.select(), element)
    await element.sendKeys(Key.BACK_SPACE)
    expect(await element.getAttribute("value")).toEqual("")
  }, timeout)

export const dropdownSelect = async (selector: string, index: number) => {
  const optionSelector = `${selector} > option[value='${index}']`
  const option = await $(optionSelector)
  option.click()
}
