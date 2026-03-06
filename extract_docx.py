import zipfile
import xml.etree.ElementTree as ET
import sys
import os

def extract_text_from_docx(docx_path, out_path):
    try:
        if not os.path.exists(docx_path):
            return f"Error: File '{docx_path}' does not exist."
            
        with zipfile.ZipFile(docx_path) as docx:
            xml_content = docx.read('word/document.xml')
            tree = ET.fromstring(xml_content)
            
            ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
            
            text = []
            for paragraph in tree.findall('.//w:p', namespaces=ns):
                texts = [node.text for node in paragraph.findall('.//w:t', namespaces=ns) if node.text]
                if texts:
                    text.append(''.join(texts))
            
            with open(out_path, 'w', encoding='utf-8') as f:
                f.write('\n'.join(text))
            return "Success"
    except Exception as e:
        return f"Error extracting text: {e}"

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: python extract_docx.py <path_to_docx> <out_path>")
    else:
        print(extract_text_from_docx(sys.argv[1], sys.argv[2]))
