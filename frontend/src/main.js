import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { install as VueMonacoEditorPlugin, loader } from '@guolao/vue-monaco-editor'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'
import App from './App.vue'
import router from './router'
import './assets/index.css'

// Monaco Editor를 로컬 번들로 로드 — 외부 CDN 의존성 제거
self.MonacoEnvironment = {
  getWorker(_, label) {
    if (label === 'json') return new jsonWorker()
    if (label === 'typescript' || label === 'javascript') return new tsWorker()
    return new editorWorker()
  }
}
loader.config({ monaco })

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(VueMonacoEditorPlugin)
app.mount('#app')
