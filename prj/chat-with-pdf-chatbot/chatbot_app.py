import streamlit as st
import os
import base64
import time
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
from transformers import pipeline
import torch
import textwrap
from langchain.document_loaders import PyPDFLoader, DirectoryLoader, PDFMinerLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.embeddings import SentenceTransformerEmbeddings
from langchain.vectorstores import Chroma
from langchain.llms import HuggingFacePipeline
from langchain.chains import RetrievalQA
from constants import CHROMA_SETTINGS
from streamlit_chat import message

st.set_page_config(layout="wide")

device = torch.device('cpu')

checkpoint = "MBZUAI/LaMini-T5-738M"

print(f"Checkpoint path:{checkpoint}")
tokenizer = AutoTokenizer.from_pretrained(checkpoint)
base_model = AutoModelForSeq2SeqLM.from_pretrained(checkpoint,
                                                   device_map=device,
                                                    torch_dtype=torch.float32
                                                    )
