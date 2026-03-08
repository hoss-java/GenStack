// console.js
class ConsoleWindow {
  constructor() {
    this.consoleWindow = document.querySelector('.console-window');
    this.consoleHeader = document.querySelector('.console-header');
    this.consoleOutput = document.querySelector('.console-output');
    this.toggleBtn = document.querySelector('.console-toggle-btn');
    this.isDragging = false;
    this.offsetX = 0;
    this.offsetY = 0;
    this.isMinimized = false;
    
    this.setupDragging();
    this.setupToggle();
  }

  setupDragging() {
    if (this.consoleHeader) {
      this.consoleHeader.addEventListener('mousedown', (e) => this.startDrag(e));
      document.addEventListener('mousemove', (e) => this.drag(e));
      document.addEventListener('mouseup', () => this.stopDrag());
      
      this.consoleHeader.addEventListener('touchstart', (e) => this.startDrag(e));
      document.addEventListener('touchmove', (e) => this.drag(e));
      document.addEventListener('touchend', () => this.stopDrag());
    }
  }

  setupToggle() {
    if (this.toggleBtn) {
      this.toggleBtn.addEventListener('click', () => this.toggleMinimize());
    }
  }

  startDrag(e) {
    this.isDragging = true;
    const rect = this.consoleWindow.getBoundingClientRect();
    const clientX = e.clientX || e.touches?.[0]?.clientX;
    const clientY = e.clientY || e.touches?.[0]?.clientY;
    this.offsetX = clientX - rect.left;
    this.offsetY = clientY - rect.top;
    this.consoleWindow.style.cursor = 'grabbing';
  }

  drag(e) {
    if (!this.isDragging) return;
    const clientX = e.clientX || e.touches?.[0]?.clientX;
    const clientY = e.clientY || e.touches?.[0]?.clientY;
    let newX = clientX - this.offsetX;
    let newY = clientY - this.offsetY;
    const maxX = window.innerWidth - this.consoleWindow.offsetWidth;
    const maxY = window.innerHeight - this.consoleWindow.offsetHeight;
    newX = Math.max(0, Math.min(newX, maxX));
    newY = Math.max(0, Math.min(newY, maxY));
    this.consoleWindow.style.bottom = 'auto';
    this.consoleWindow.style.right = 'auto';
    this.consoleWindow.style.left = newX + 'px';
    this.consoleWindow.style.top = newY + 'px';
  }

  stopDrag() {
    this.isDragging = false;
    this.consoleWindow.style.cursor = 'default';
  }

  toggleMinimize() {
    this.isMinimized = !this.isMinimized;
    if (this.isMinimized) {
      this.consoleWindow.classList.add('minimized');
      this.toggleBtn.textContent = '📋';
    } else {
      this.consoleWindow.classList.remove('minimized');
      this.toggleBtn.textContent = '✕';
    }
  }

  addLog(message, type = 'log') {
    if (!this.consoleOutput) return;
    const logElement = document.createElement('div');
    logElement.className = `console-log ${type}`;
    const timestamp = new Date().toLocaleTimeString();
    logElement.innerHTML = `<span class="console-timestamp">[${timestamp}]</span>${message}`;
    this.consoleOutput.appendChild(logElement);
    this.consoleOutput.scrollTop = this.consoleOutput.scrollHeight;
  }

  clear() {
    if (this.consoleOutput) {
      this.consoleOutput.innerHTML = '';
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.consoleWindowInstance = new ConsoleWindow();
});
