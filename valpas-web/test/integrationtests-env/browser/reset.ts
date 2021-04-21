import { By, Key, until } from "selenium-webdriver"
import { clickElement, textEventuallyEquals } from "./content"
import { $, deleteCookies, goToLocation } from "./core"
import { driver } from "./driver"
import { getTextInput, setTextInput } from "./forms"

export const loginAs = async (
  initialPath: string,
  username: string,
  password?: string
) => {
  await reset(initialPath)
  ;(await $("#username")).sendKeys(username)
  ;(await $("#password")).sendKeys(password || username, Key.ENTER)
  await driver.wait(
    until.elementLocated(By.css("article.page:not(#login-app)")),
    5000
  )
  await driver.wait(until.elementLocated(By.css("article.page")), 5000)
}

export const defaultLogin = async (initialPath: string) =>
  loginAs(initialPath, "valpas-helsinki", "valpas-helsinki")

export const reset = async (initialPath: string) => {
  await deleteCookies()
  await goToLocation(initialPath)
  await driver.wait(until.elementLocated(By.css("article")), 5000)
  await resetMockData()
}

export const resetMockData = async (tarkasteluPäivä: string = "2021-09-05") => {
  const inputSelector = "#tarkasteluPäivä"

  const currentTarkastelupäivä = await getTextInput(inputSelector)
  const currentFixture = await (await $("#current-fixture")).getText()

  if (
    currentTarkastelupäivä !== tarkasteluPäivä ||
    currentFixture !== "VALPAS"
  ) {
    await setTextInput(inputSelector, tarkasteluPäivä)
    await clickElement("#resetMockData")
    await textEventuallyEquals("#resetMockDataState", "success", 15000)
  }
}

export const clearMockData = async () => {
  await clickElement("#clearMockData")
  await textEventuallyEquals("#clearMockDataState", "success", 15000)
}
