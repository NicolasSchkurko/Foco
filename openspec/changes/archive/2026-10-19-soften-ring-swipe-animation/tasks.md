# Tasks — soften-ring-swipe-animation

- [x] 1. Add `RingMotion` with the 320 ms base, 1/8 slide divisor, ease-out and a duration scaled by
      the system animator setting; add `RingMotionTest`.
- [x] 2. Add `data/MotionScale` reading `Settings.Global.ANIMATOR_DURATION_SCALE` once per home
      composition.
- [x] 3. Add the `softMotion` flag in `DashboardScreen`: swipe/arrow moves set it; bar/calendar/back
      keep the existing transition; scale 0 uses no transition.
- [x] 4. Use the soft tween and 1/8 slide in `AnimatedContent` for the soft branch; keep the header
      and arrows outside the animated content.
- [ ] 5. Run `gradlew.bat test`, `DashboardRingTest` and `RingMotionTest`; verify a swipe and an arrow
      on the Pixel with animations on and with the system animation scale at 0.
- [x] 6. Run `openspec validate soften-ring-swipe-animation --strict` and
      `node scripts/gates/build-board.mjs`.
