#!/usr/bin/env python3
"""
Extract training data from CV PDFs and customer request documents.
This prepares data for enhancing the Gemma3 CV-expert model.

Usage:
    python extract-training-data.py --cv-folder /path/to/cvs --output training-examples.txt
"""

import os
import sys
import argparse
from pathlib import Path
import json

try:
    import PyPDF2
    from docx import Document
except ImportError:
    print("❌ Missing required packages. Install with:")
    print("   pip install PyPDF2 python-docx")
    sys.exit(1)


def extract_text_from_pdf(pdf_path):
    """Extract text from PDF file."""
    try:
        with open(pdf_path, 'rb') as file:
            reader = PyPDF2.PdfReader(file)
            text = ""
            for page in reader.pages:
                text += page.extract_text() + "\n"
            return text.strip()
    except Exception as e:
        print(f"⚠️  Error reading {pdf_path}: {e}")
        return None


def extract_text_from_docx(docx_path):
    """Extract text from Word document."""
    try:
        doc = Document(docx_path)
        text = "\n".join([paragraph.text for paragraph in doc.paragraphs])
        return text.strip()
    except Exception as e:
        print(f"⚠️  Error reading {docx_path}: {e}")
        return None


def extract_text_from_file(file_path):
    """Extract text from PDF or Word document."""
    file_path = Path(file_path)
    
    if file_path.suffix.lower() == '.pdf':
        return extract_text_from_pdf(file_path)
    elif file_path.suffix.lower() in ['.docx', '.doc']:
        return extract_text_from_docx(file_path)
    else:
        print(f"⚠️  Skipping unsupported file: {file_path}")
        return None


def create_few_shot_example(cv_text, document_type="CV"):
    """
    Create a few-shot example format for the Modelfile.
    Returns a formatted string ready to paste into Modelfile.
    """
    # Truncate very long CVs
    max_length = 2000
    if len(cv_text) > max_length:
        cv_text = cv_text[:max_length] + "..."
    
    example = f'''
# Example {document_type}
MESSAGE user """Evaluate this {document_type}:

{cv_text}"""

MESSAGE assistant """{{
  "scorePercentage": [SCORE_HERE],
  "summary": "[SUMMARY_HERE]",
  "strengths": [
    "[STRENGTH_1]",
    "[STRENGTH_2]",
    "[STRENGTH_3]"
  ],
  "improvements": [
    "[IMPROVEMENT_1]",
    "[IMPROVEMENT_2]",
    "[IMPROVEMENT_3]"
  ]
}}"""
'''
    return example


def process_folder(folder_path, document_type="CV"):
    """Process all PDFs and Word docs in a folder."""
    folder = Path(folder_path)
    
    if not folder.exists():
        print(f"❌ Folder not found: {folder}")
        return []
    
    examples = []
    files = list(folder.glob('*.pdf')) + list(folder.glob('*.docx')) + list(folder.glob('*.doc'))
    
    print(f"\n📁 Processing {len(files)} files from {folder}...")
    
    for file_path in files:
        print(f"   📄 {file_path.name}...", end=" ")
        text = extract_text_from_file(file_path)
        
        if text:
            example = create_few_shot_example(text, document_type)
            examples.append({
                'file': str(file_path.name),
                'text': text,
                'example': example
            })
            print("✅")
        else:
            print("❌")
    
    return examples


def main():
    parser = argparse.ArgumentParser(
        description='Extract training data from CV and customer request documents'
    )
    parser.add_argument('--cv-folder', help='Folder containing CV PDFs/Word docs')
    parser.add_argument('--request-folder', help='Folder containing customer request PDFs/Word docs')
    parser.add_argument('--output', default='training-examples.txt', help='Output file for examples')
    parser.add_argument('--modelfile-output', default='Modelfile-examples.txt', 
                       help='Output file for Modelfile-ready examples')
    
    args = parser.parse_args()
    
    if not args.cv_folder and not args.request_folder:
        print("❌ Please provide at least --cv-folder or --request-folder")
        parser.print_help()
        return
    
    all_examples = []
    
    # Process CVs
    if args.cv_folder:
        cv_examples = process_folder(args.cv_folder, document_type="CV")
        all_examples.extend(cv_examples)
        print(f"✅ Extracted {len(cv_examples)} CV examples")
    
    # Process customer requests
    if args.request_folder:
        request_examples = process_folder(args.request_folder, document_type="Customer Request")
        all_examples.extend(request_examples)
        print(f"✅ Extracted {len(request_examples)} customer request examples")
    
    # Save full text
    with open(args.output, 'w', encoding='utf-8') as f:
        for idx, example in enumerate(all_examples, 1):
            f.write(f"{'='*80}\n")
            f.write(f"Document {idx}: {example['file']}\n")
            f.write(f"{'='*80}\n")
            f.write(example['text'])
            f.write(f"\n\n")
    
    print(f"\n📝 Full text saved to: {args.output}")
    
    # Save Modelfile-ready examples
    with open(args.modelfile_output, 'w', encoding='utf-8') as f:
        f.write("# Few-shot examples extracted from your documents\n")
        f.write("# Copy these into your Modelfile after manually scoring them\n")
        f.write(f"# Total examples: {len(all_examples)}\n\n")
        
        for idx, example in enumerate(all_examples, 1):
            f.write(f"# From: {example['file']}\n")
            f.write(example['example'])
            f.write("\n\n")
    
    print(f"📝 Modelfile examples saved to: {args.modelfile_output}")
    
    print("\n" + "="*80)
    print("📚 Next Steps:")
    print("="*80)
    print(f"1. Review the extracted text in '{args.output}'")
    print(f"2. Manually score 5-10 examples (add scores, summaries, strengths, improvements)")
    print(f"3. Copy the best scored examples from '{args.modelfile_output}' to your Modelfile")
    print(f"4. Recreate the model:")
    print(f"   ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert --force")
    print(f"5. Test the improved model:")
    print(f"   ./ollama-models/test-cv-expert.sh")
    print("="*80)
    
    # Create a sample scoring template
    if len(all_examples) > 0:
        print(f"\n📋 Sample scoring template (for {all_examples[0]['file']}):\n")
        print("Copy this, fill in the scores, and add to Modelfile:\n")
        print(all_examples[0]['example'])


if __name__ == '__main__':
    main()
