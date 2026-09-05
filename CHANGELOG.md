# Changelog

## [0.6.0](https://github.com/srednimax/gloam/compare/v0.5.0...v0.6.0) (2026-09-05)


### Features

* dim on a nightly schedule ([b961b9a](https://github.com/srednimax/gloam/commit/b961b9ad7b1c8419cf101b0c95349080ecc3e95f))
* put the icon's ground on the night instead of the mud ([159113b](https://github.com/srednimax/gloam/commit/159113b9dd5ffd296825c9d444d92f53167cee02))
* replace the placeholder mark with the moon ([2feadf0](https://github.com/srednimax/gloam/commit/2feadf0fe6eedc216cfac26dc2b9f4028f2544f5))


### Bug Fixes

* keep the feature graphic inside Play's 16:9 crop ([f976de4](https://github.com/srednimax/gloam/commit/f976de4202d1af51e0cd5f1abda38b6d64a283bf))
* sweep the two call sites the refactor's sweep missed ([0332a88](https://github.com/srednimax/gloam/commit/0332a88cff0a5a9d9ce415b005904b637c324e6b))
* take the shade down when the screen comes on, not a minute later ([e779c38](https://github.com/srednimax/gloam/commit/e779c38faf5c518f18c1672e3e5e5e248e52e124))

## [0.5.0](https://github.com/srednimax/gloam/compare/v0.4.0...v0.5.0) (2026-09-04)


### Features

* add the compact controls ([744d9df](https://github.com/srednimax/gloam/commit/744d9dfd9d8df0336996a0156629f7345388d309))
* give the compact controls a bar and three buttons ([c87ad8f](https://github.com/srednimax/gloam/commit/c87ad8fa5d1b2442ebd4aff43c3f50d2d05fe24d))
* open the compact controls from the notification and the icon ([3c2146e](https://github.com/srednimax/gloam/commit/3c2146e04146aa7b1e372d16109c1ac97dcb4f26))
* put the controls in a window above the shade ([012459c](https://github.com/srednimax/gloam/commit/012459ca4d64f44dcc11fdf135d21c0309ce5a3f))
* say in the notification that the brightness slider is paused ([2e3f908](https://github.com/srednimax/gloam/commit/2e3f908045b3ccfbead56a7733c0159e7d0f8af8))


### Bug Fixes

* honour the icon preference when Gloam's task is already open ([94e5396](https://github.com/srednimax/gloam/commit/94e53969acf5b8e32444ab31030ad08ed4450209))
* re-measure the panel when the display turns ([74ea7cc](https://github.com/srednimax/gloam/commit/74ea7cc5cccfc0afee84e95006452a1e45a9ed01))
* stop a summon from raising the shade the user turned off ([eb3d33a](https://github.com/srednimax/gloam/commit/eb3d33acaab2ba71698496945a1c5ee25b8b038a))
* stop counting the navigation bar twice under the panel ([23fc329](https://github.com/srednimax/gloam/commit/23fc329ccf0e342d6c3a16b06f63707a1f27a554))

## [0.4.0](https://github.com/srednimax/gloam/compare/v0.3.0...v0.4.0) (2026-09-01)


### Features

* add a Help and feedback screen ([ad06566](https://github.com/srednimax/gloam/commit/ad065661fdd5a365fb7777388f7e0615e3fb9251))
* put the shade back after a restart ([872e743](https://github.com/srednimax/gloam/commit/872e743167fd08fcd8a241b95d8b70504817443a))
* take the shade down on its own after a while ([1231b17](https://github.com/srednimax/gloam/commit/1231b17106451dca34a287e6b57015052710801e))


### Bug Fixes

* log the restore under a tag the platform does not already use ([280caa6](https://github.com/srednimax/gloam/commit/280caa62043167fee52fa43229ee3310d5e5114a))
* make device-gate report what it actually read ([778cbc1](https://github.com/srednimax/gloam/commit/778cbc1f92bf5d5ff50c48defdac5387f653e70a))
* point device-gate at the label the phone actually shows ([50a7017](https://github.com/srednimax/gloam/commit/50a701772f8f19e3ac988fb9b8a7d6fa5e94aca5))

## [0.3.0](https://github.com/srednimax/gloam/compare/v0.2.0...v0.3.0) (2026-08-31)


### Features

* ask for notification permission before the first shade ([ad2fe81](https://github.com/srednimax/gloam/commit/ad2fe81d8ecfd911541617e17caf62408a89d790))
* take the backlight down with the dim level ([1f6720e](https://github.com/srednimax/gloam/commit/1f6720e076dee98cee8619e75017f12ff9ecae6f))
* tint the shade amber ([6a56ccc](https://github.com/srednimax/gloam/commit/6a56cccd2bea332307bfcacdff70eb881c09b2bf))

## [0.2.0](https://github.com/srednimax/gloam/compare/v0.1.0...v0.2.0) (2026-08-30)


### ⚠ BREAKING CHANGES

* devices below Android 13 (API 33) can no longer install Gloam. Roughly 30% of Android devices are excluded. Nothing is published yet, so no existing install is stranded.

### Features

* bootstrap the template into Gloam ([4fd4392](https://github.com/srednimax/gloam/commit/4fd43925d623fd0af72d309cd072e82fa19092ec))
* give Gloam a dusk palette instead of the template's slate blue ([ad4003e](https://github.com/srednimax/gloam/commit/ad4003e5877f4ef2fe921b7647fe3ce6ef7d406e))
* require Android 13, raising minSdk from 26 to 33 ([5dd8afa](https://github.com/srednimax/gloam/commit/5dd8afaa9e5b8f2c78b08e150afca6cf0387c2e3))
* strip the template's data layer and add the dimming shade ([e2ebc68](https://github.com/srednimax/gloam/commit/e2ebc68e84cbd06d43acc15bf138344c4abc6f26))


### Bug Fixes

* carry the shade over the status bar on HyperOS ([b8e8c35](https://github.com/srednimax/gloam/commit/b8e8c35b8d3e1608176fa2bdddff52d896c196ef))
* correct the artifact permission inventory to Gloam's own set ([f7b016e](https://github.com/srednimax/gloam/commit/f7b016e41a3745cb602fd83a3627d9dabe36576f))

## Changelog

Nothing released yet.
