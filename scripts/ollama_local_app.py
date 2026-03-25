#!/usr/bin/env python3
"""
Small local Ollama app for chatting and generating embeddings.

Usage examples:
  python3 scripts/ollama_local_app.py --self-test
  python3 scripts/ollama_local_app.py --chat-model qwen2.5:14b
  python3 scripts/ollama_local_app.py --embed "Senior Kotlin developer with Spring Boot and PostgreSQL"
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


DEFAULT_BASE_URL = "http://localhost:11434"
DEFAULT_CHAT_MODEL = "qwen2.5:14b"
DEFAULT_EMBED_MODEL = "bge-m3:latest"


@dataclass
class OllamaClient:
    base_url: str

    def _post_json(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        body = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url=f"{self.base_url.rstrip('/')}{path}",
            data=body,
            method="POST",
            headers={"Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(req, timeout=120) as response:
                raw = response.read().decode("utf-8")
                return json.loads(raw)
        except urllib.error.HTTPError as e:
            err = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"HTTP {e.code} on {path}: {err}") from e
        except urllib.error.URLError as e:
            raise RuntimeError(
                f"Could not connect to Ollama at {self.base_url}. Is it running?"
            ) from e

    def _get_json(self, path: str) -> dict[str, Any]:
        req = urllib.request.Request(
            url=f"{self.base_url.rstrip('/')}{path}",
            method="GET",
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                raw = response.read().decode("utf-8")
                return json.loads(raw)
        except urllib.error.HTTPError as e:
            err = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"HTTP {e.code} on {path}: {err}") from e
        except urllib.error.URLError as e:
            raise RuntimeError(
                f"Could not connect to Ollama at {self.base_url}. Is it running?"
            ) from e

    def version(self) -> str:
        data = self._get_json("/api/version")
        return str(data.get("version", "unknown"))

    def list_models(self) -> list[str]:
        data = self._get_json("/api/tags")
        models = data.get("models", [])
        return [str(m.get("name")) for m in models if m.get("name")]

    def chat(self, model: str, messages: list[dict[str, str]]) -> str:
        payload = {
            "model": model,
            "messages": messages,
            "stream": False,
        }
        data = self._post_json("/api/chat", payload)
        return str(data.get("message", {}).get("content", "")).strip()

    def embed(self, model: str, text: str) -> list[float]:
        payload = {
            "model": model,
            "input": text,
        }
        data = self._post_json("/api/embed", payload)
        vectors = data.get("embeddings", [])
        if not vectors:
            raise RuntimeError("No embeddings returned by Ollama.")
        first = vectors[0]
        if not isinstance(first, list):
            raise RuntimeError("Unexpected embedding response format.")
        return [float(x) for x in first]


def run_self_test(client: OllamaClient, chat_model: str, embed_model: str) -> int:
    print(f"Ollama version: {client.version()}")
    models = client.list_models()
    print(f"Installed models ({len(models)}): {', '.join(models)}")

    if chat_model not in models:
        print(f"WARNING: chat model '{chat_model}' is not in /api/tags output.")
    if embed_model not in models:
        print(f"WARNING: embedding model '{embed_model}' is not in /api/tags output.")

    reply = client.chat(chat_model, [{"role": "user", "content": "Reply with exactly: ok"}])
    print(f"Chat test [{chat_model}]: {reply}")

    vector = client.embed(embed_model, "semantic search test")
    print(f"Embedding test [{embed_model}]: dimension={len(vector)}, first3={vector[:3]}")
    return 0


def run_embed_only(client: OllamaClient, embed_model: str, text: str) -> int:
    vector = client.embed(embed_model, text)
    print(json.dumps({"model": embed_model, "dimension": len(vector), "embedding": vector}))
    return 0


def run_chat_repl(client: OllamaClient, chat_model: str, embed_model: str) -> int:
    print(f"Connected to Ollama at {client.base_url}")
    print(f"Chat model: {chat_model}")
    print(f"Embedding model: {embed_model}")
    print("Commands:")
    print("  /embed <text>  -> generate embedding and print dimension")
    print("  /models        -> list local models")
    print("  /quit          -> exit")
    print("Type your message to chat.")

    messages: list[dict[str, str]] = []
    while True:
        try:
            user_input = input("\nYou> ").strip()
        except EOFError:
            print("\nBye.")
            return 0

        if not user_input:
            continue
        if user_input in ("/quit", "/exit"):
            print("Bye.")
            return 0
        if user_input == "/models":
            print("\n".join(client.list_models()))
            continue
        if user_input.startswith("/embed "):
            text = user_input[len("/embed ") :].strip()
            if not text:
                print("Please provide text after /embed.")
                continue
            vector = client.embed(embed_model, text)
            print(f"Embedding dimension={len(vector)} first5={vector[:5]}")
            continue

        messages.append({"role": "user", "content": user_input})
        answer = client.chat(chat_model, messages)
        messages.append({"role": "assistant", "content": answer})
        print(f"Bot> {answer}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Local Ollama chat + embedding app")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="Ollama base URL")
    parser.add_argument("--chat-model", default=DEFAULT_CHAT_MODEL, help="Model for /api/chat")
    parser.add_argument("--embed-model", default=DEFAULT_EMBED_MODEL, help="Model for /api/embed")
    parser.add_argument("--embed", default=None, help="Generate embedding for text and exit")
    parser.add_argument("--self-test", action="store_true", help="Run connectivity/model tests and exit")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    client = OllamaClient(base_url=args.base_url)
    try:
        if args.self_test:
            return run_self_test(client, args.chat_model, args.embed_model)
        if args.embed is not None:
            return run_embed_only(client, args.embed_model, args.embed)
        return run_chat_repl(client, args.chat_model, args.embed_model)
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
