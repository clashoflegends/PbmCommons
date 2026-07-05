## How to build

**Prerequisites**
- JDK 8 (tested with 1.8.0_301 / 1.8.0_461; use the JDK, not just JRE)
- Apache Ant 1.8.0 or higher

**Steps**

1. Clone this repo:
   ```
   git clone https://github.com/clashoflegends/PbmCommons
   ```

2. Build:
   ```
   ant jar
   ```
   Output lands in `dist/PbmCommons.jar`.

**Note:** `lib/PbmPersistenceCommons.jar` is a prebuilt binary committed to this repo (private source). The build uses it as-is.

---

PbmCommons is the shared library used by all Clash of Legends Java projects (Counselor, Judge, and the pipeline tools). It contains the game model, business logic, GUI utilities, i18n resources, and shared images.

---

## License

PbmCommons is released under the [MIT License](LICENSE). © 2004-2026 Clash of Legends.
