import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: false
})

export function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}
