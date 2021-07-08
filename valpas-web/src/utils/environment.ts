const runningOnJest = process.env.JEST_WORKER_ID !== undefined

export const animationsDisabled = () => process.env.NODE_ENV === "test"

export const runningLocally = (): boolean =>
  runningOnJest || ["local", "unittest"].includes(window.environment || "")
