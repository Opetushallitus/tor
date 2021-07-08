const kill = require("tree-kill")

module.exports = () => {
  kill(global.__PARCEL_SERVE_PROCESS__.pid)
}
