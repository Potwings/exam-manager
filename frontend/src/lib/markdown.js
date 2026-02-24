import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'

hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('sql', sql)

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: false,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<span class="code-lang-label">${lang}</span>` +
          hljs.highlight(str, { language: lang }).value
      } catch (_) {}
    }
    return ''
  }
})

export function renderMarkdown(text) {
  if (!text) return ''
  const html = md.render(text)
  return groupConsecutiveTables(html)
}

/**
 * 연속된 테이블(제목 포함)을 flex 컨테이너로 감싸서 한 줄에 나란히 표시.
 * <p>제목</p><table>...</table> 쌍이 2개 이상 연속되면 그룹핑.
 */
function groupConsecutiveTables(html) {
  if (!html.includes('<table>')) return html

  const div = document.createElement('div')
  div.innerHTML = html

  const children = Array.from(div.children)
  const groups = []

  let i = 0
  while (i < children.length) {
    const group = []
    let j = i

    while (j < children.length) {
      const el = children[j]
      if (el.tagName === 'TABLE') {
        group.push({ headingIdx: null, tableIdx: j })
        j++
      } else if (
        isTableHeading(el) &&
        j + 1 < children.length &&
        children[j + 1].tagName === 'TABLE'
      ) {
        group.push({ headingIdx: j, tableIdx: j + 1 })
        j += 2
      } else {
        break
      }
    }

    if (group.length >= 2) {
      groups.push(group)
      i = j
    } else {
      i = j > i ? j : i + 1
    }
  }

  if (groups.length === 0) return html

  for (let g = groups.length - 1; g >= 0; g--) {
    const group = groups[g]
    const wrapper = document.createElement('div')
    wrapper.className = 'table-group'

    const firstIdx = group[0].headingIdx ?? group[0].tableIdx
    div.insertBefore(wrapper, children[firstIdx])

    for (const { headingIdx, tableIdx } of group) {
      const cell = document.createElement('div')
      if (headingIdx !== null) cell.appendChild(children[headingIdx])
      cell.appendChild(children[tableIdx])
      wrapper.appendChild(cell)
    }
  }

  return div.innerHTML
}

function isTableHeading(el) {
  return (el.tagName === 'P' || /^H[1-6]$/.test(el.tagName)) &&
    el.textContent.trim().length < 100
}
