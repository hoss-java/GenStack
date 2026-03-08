// consoleLogger.js
class ConsoleLogger {
  constructor() {
    this.consoleWindow = window.consoleWindowInstance;
    this.setupInterception();
  }

  // Simple and reliable stack trace parsing
  getCallerInfo() {
    try {
      const stack = new Error().stack;
      if (!stack) return { method: 'unknown', line: '?' };

      const lines = stack.split('\n');
      
      // Log all stack lines to debug (you can remove this later)
      // console.log('Stack:', lines);
      
      // Try to find the first line that's NOT from consoleLogger.js
      let callerLine = null;
      for (let i = 1; i < lines.length; i++) {
        if (!lines[i].includes('consoleLogger') && !lines[i].includes('getCallerInfo')) {
          callerLine = lines[i];
          break;
        }
      }

      if (!callerLine) callerLine = lines[3] || lines[2];

      // Extract function name: "at functionName" or "at Object.functionName"
      let method = 'anonymous';
      const methodMatch = callerLine.match(/at\s+(?:new\s+)?(?:Object\.)?([a-zA-Z_$][a-zA-Z0-9_$]*)/);
      if (methodMatch) {
        method = methodMatch[1];
      }

      // Extract line number: "file.js:123:45"
      let line = '?';
      const lineMatch = callerLine.match(/:(\d+):/);
      if (lineMatch) {
        line = lineMatch[1];
      }

      // Extract file name
      let file = '?';
      const fileMatch = callerLine.match(/\(([^/:]+):/);
      if (fileMatch) {
        file = fileMatch[1];
      }

      return { method, line, file };
    } catch (e) {
      return { method: 'unknown', line: '?' };
    }
  }

  setupInterception() {
    const originalLog = console.log;
    const originalError = console.error;
    const originalWarn = console.warn;
    const originalInfo = console.info;

    console.log = (...args) => {
      originalLog(...args);
      const caller = this.getCallerInfo();
      this.logToWindow(args.join(' '), 'log', caller);
    };

    console.error = (...args) => {
      originalError(...args);
      const caller = this.getCallerInfo();
      this.logToWindow(args.join(' '), 'error', caller);
    };

    console.warn = (...args) => {
      originalWarn(...args);
      const caller = this.getCallerInfo();
      this.logToWindow(args.join(' '), 'warn', caller);
    };

    console.info = (...args) => {
      originalInfo(...args);
      const caller = this.getCallerInfo();
      this.logToWindow(args.join(' '), 'log', caller);
    };
  }

  logToWindow(message, type = 'log', caller = {}) {
    if (this.consoleWindow) {
      const { method = 'unknown', line = '?', file = '?' } = caller;
      const formattedMessage = `[${file} ${method}:${line}] ${message}`;
      this.consoleWindow.addLog(formattedMessage, type);
    }
  }

  log(message, type = 'log') {
    const caller = this.getCallerInfo();
    console[type === 'log' ? 'log' : type](message);
    this.logToWindow(message, type, caller);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new ConsoleLogger();
});
