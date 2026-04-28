<template>
  <div class="bpmn-editor">
    <div class="bpmn-editor__main">
      <div class="bpmn-editor__workspace">
        <div class="bpmn-editor__toolbar">
          <el-button size="mini" @click="handleReset">重置流程</el-button>
          <el-button size="mini" @click="handleFit">适配画布</el-button>
        </div>
        <div ref="canvas" class="bpmn-editor__canvas" />
      </div>
      <div class="bpmn-editor__panel">
        <div class="bpmn-editor__panel-title">属性配置</div>
        <div v-if="selectedElement" class="bpmn-editor__panel-body">
          <el-form label-position="top" size="mini">
            <el-form-item label="节点类型"><el-input :value="selectedElement.type" disabled /></el-form-item>
            <el-form-item label="节点ID"><el-input :value="selectedElement.id" @input="updateId" /></el-form-item>
            <el-form-item label="显示名称"><el-input :value="selectedName" @input="updateName" /></el-form-item>
          </el-form>

          <!-- Process 流程级别表单字段配置 -->
          <template v-if="isProcess">
            <el-divider content-position="left">流程表单字段</el-divider>
            <div class="bpmn-editor__hint" style="margin-bottom:10px;">配置申请人填写的表单字段</div>
            <div class="bpmn-editor__field-list">
              <div v-for="(field, index) in processFieldDraft" :key="index" class="bpmn-editor__field-card">
                <el-form label-width="auto" size="mini">
                  <div class="bpmn-editor__field-grid">
                    <el-form-item label="字段编码">
                      <el-input v-model="field.code" size="mini" @input="updateFieldDraft('process', index, 'code', field.code)" />
                    </el-form-item>
                    <el-form-item label="字段名称">
                      <el-input v-model="field.label" size="mini" @input="updateFieldDraft('process', index, 'label', field.label)" />
                    </el-form-item>
                    <el-form-item label="字段类型">
                      <el-select v-model="field.type" size="mini" @change="updateFieldDraft('process', index, 'type', field.type)">
                        <el-option v-for="opt in fieldTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="是否必填">
                      <el-switch v-model="field.required" @change="updateFieldDraft('process', index, 'required', field.required)" />
                    </el-form-item>
                    <el-form-item label="显示阶段">
                      <el-select v-model="field.displayStage" size="mini" @change="updateFieldDraft('process', index, 'displayStage', field.displayStage)">
                        <el-option label="申请+审批" value="both" />
                        <el-option label="仅申请" value="apply" />
                        <el-option label="仅审批" value="approve" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="审批只读">
                      <el-switch v-model="field.approveReadonly" @change="updateFieldDraft('process', index, 'approveReadonly', field.approveReadonly)" />
                    </el-form-item>
                  </div>
                  <el-form-item v-if="field.type === 'select'" label="选项配置">
                    <el-input v-model="field.optionsText" type="textarea" :rows="3" size="mini" placeholder="每行一个选项，格式：值|显示名" @input="updateFieldDraft('process', index, 'optionsText', field.optionsText)" />
                  </el-form-item>
                </el-form>  
                <div class="bpmn-editor__field-actions">
                  <el-button type="text" size="mini" @click="removeFieldDraft('process', index)">删除</el-button>
                </div>
              </div>
            </div>
            <el-button type="primary" size="mini" plain @click="addFieldDraft('process')">添加字段</el-button>
          </template>

          <!-- UserTask 用户任务配置 -->
          <template v-if="isUserTask">
            <el-divider content-position="left">审批人配置</el-divider>
            <el-form label-position="top" size="mini">
              <el-form-item label="审批人类型">
                <el-select :value="userTaskApproverType" size="mini" @change="updateUserTaskAttr('lx:approverType', $event)">
                  <el-option label="指定用户" value="user" />
                  <el-option label="指定角色" value="role" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="userTaskApproverType === 'role'" label="审批角色">
                <el-select :value="userTaskApproverValue" size="mini" filterable @change="updateUserTaskAttr('lx:approverValue', $event)">
                  <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
                </el-select>
              </el-form-item>
              <el-form-item v-else label="审批值">
                <el-input :value="userTaskApproverValue" size="mini" @input="updateUserTaskAttr('lx:approverValue', $event)" />
              </el-form-item>
              <el-form-item label="表单Key">
                <el-input :value="userTaskFormKey" size="mini" @input="updateUserTaskAttr('flowable:formKey', $event)" />
              </el-form-item>
            </el-form>

            <el-divider content-position="left">审批表单字段</el-divider>
            <div class="bpmn-editor__hint" style="margin-bottom:10px;">配置审批人可见/可编辑的字段</div>
            <div class="bpmn-editor__field-list">
              <div v-for="(field, index) in userTaskFieldDraft" :key="index" class="bpmn-editor__field-card">
                <el-form label-width="auto" size="mini">
                  <div class="bpmn-editor__field-grid">
                    <el-form-item label="字段编码">
                      <el-input v-model="field.code" size="mini" @input="updateFieldDraft('userTask', index, 'code', field.code)" />
                    </el-form-item>
                    <el-form-item label="字段名称">
                      <el-input v-model="field.label" size="mini" @input="updateFieldDraft('userTask', index, 'label', field.label)" />
                    </el-form-item>
                    <el-form-item label="字段类型">
                      <el-select v-model="field.type" size="mini" @change="updateFieldDraft('userTask', index, 'type', field.type)">
                        <el-option v-for="opt in fieldTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="是否必填">
                      <el-switch v-model="field.required" @change="updateFieldDraft('userTask', index, 'required', field.required)" />
                    </el-form-item>
                    <el-form-item label="显示阶段">
                      <el-select v-model="field.displayStage" size="mini" @change="updateFieldDraft('userTask', index, 'displayStage', field.displayStage)">
                        <el-option label="申请+审批" value="both" />
                        <el-option label="仅申请" value="apply" />
                        <el-option label="仅审批" value="approve" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="审批只读">
                      <el-switch v-model="field.approveReadonly" @change="updateFieldDraft('userTask', index, 'approveReadonly', field.approveReadonly)" />
                    </el-form-item>
                  </div>
                  <el-form-item v-if="field.type === 'select'" label="选项配置">
                    <el-input v-model="field.optionsText" type="textarea" :rows="3" size="mini" placeholder="每行一个选项，格式：值|显示名" @input="updateFieldDraft('userTask', index, 'optionsText', field.optionsText)" />
                  </el-form-item>
                </el-form>
                <div class="bpmn-editor__field-actions">
                  <el-button type="text" size="mini" @click="removeFieldDraft('userTask', index)">删除</el-button>
                </div>
              </div>
            </div>
            <el-button type="primary" size="mini" plain @click="addFieldDraft('userTask')">添加字段</el-button>
          </template>

          <!-- SequenceFlow 条件表达式配置 -->
          <template v-if="isSequenceFlow">
            <el-divider content-position="left">条件配置</el-divider>
            <el-form label-position="top" size="mini">
              <el-form-item label="条件说明">
                <el-input :value="sequenceConditionSummary" size="mini" @input="updateSequenceAttr('lx:conditionSummary', $event)" />
              </el-form-item>
              <el-form-item label="条件表达式">
                <el-input :value="sequenceCondition" size="mini" placeholder="${field > value}" @input="updateSequenceCondition($event)" />
              </el-form-item>
            </el-form>
            <div class="bpmn-editor__condition-builder">
              <el-select v-model="sequenceBuilder.field" size="mini" placeholder="字段" @change="updateSequenceBuilder('field', $event)">
                <el-option v-for="opt in sequenceFieldOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-select v-model="sequenceBuilder.operator" size="mini" placeholder="运算符" @change="updateSequenceBuilder('operator', $event)">
                <el-option v-for="opt in conditionOperatorOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-input v-model="sequenceBuilder.value" size="mini" placeholder="值" @input="updateSequenceBuilder('value', $event)" />
            </div>
            <div class="bpmn-editor__condition-actions">
              <el-button type="primary" size="mini" @click="applySequenceBuilder">生成表达式</el-button>
            </div>
            <div class="bpmn-editor__condition-tip">选择字段、运算符和值后点击"生成表达式"</div>
          </template>
        </div>
        <div v-else class="bpmn-editor__empty">请点击画布中的节点进行配置。</div>
      </div>
    </div>
  </div>
</template>

<script>
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { getBusinessObject, is } from 'bpmn-js/lib/util/ModelUtil'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { listRole } from '@/api/system/role'
import flowableModdle from './flowable-moddle.json'
import lingxiModdle from './lingxi-moddle.json'

export default {
  name: 'BpmnEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    processKey: {
      type: String,
      default: 'oa_process'
    },
    processName: {
      type: String,
      default: 'OA Process'
    }
  },
  data() {
    return {
      modeler: null,
      currentXml: '',
      syncing: false,
      selectedElement: null,
      selectionVersion: 0,
      processFieldDraft: [],
      userTaskFieldDraft: [],
      roleOptions: [],
      sequenceBuilder: {
        field: '',
        operator: '',
        value: ''
      },
      fieldTypeOptions: [
        { label: 'text', value: 'text' },
        { label: 'textarea', value: 'textarea' },
        { label: 'number', value: 'number' },
        { label: 'date', value: 'date' },
        { label: 'select', value: 'select' }
      ],
      conditionOperatorOptions: [
        { label: '==', value: '==' },
        { label: '!=', value: '!=' },
        { label: '>', value: '>' },
        { label: '>=', value: '>=' },
        { label: '<', value: '<' },
        { label: '<=', value: '<=' }
      ]
    }
  },
  computed: {
    selectedBo() {
      if (!this.selectedElement) {
        return null
      }
      return getBusinessObject(this.selectedElement)
    },
    selectedName() {
      return this.selectedBo && this.selectedBo.name ? this.selectedBo.name : ''
    },
    isProcess() {
      return this.selectedElement && is(this.selectedElement, 'bpmn:Process')
    },
    isUserTask() {
      return this.selectedElement && is(this.selectedElement, 'bpmn:UserTask')
    },
    isSequenceFlow() {
      return this.selectedElement && is(this.selectedElement, 'bpmn:SequenceFlow')
    },
    userTaskApproverType() {
      return this.readAttr('lx:approverType') || 'user'
    },
    userTaskApproverValue() {
      return this.readAttr('lx:approverValue')
    },
    userTaskFormKey() {
      return this.readAttr('flowable:formKey')
    },
    sequenceCondition() {
      if (!this.selectedBo || !this.selectedBo.conditionExpression) {
        return ''
      }
      return this.selectedBo.conditionExpression.body || ''
    },
    sequenceConditionSummary() {
      return this.readAttr('lx:conditionSummary')
    },
    sequenceFieldOptions() {
      const defaults = [
        { value: 'amount', label: 'amount(amount)', type: 'number' },
        { value: 'expenseType', label: 'expenseType(expenseType)', type: 'select' },
        { value: 'leaveDays', label: 'leaveDays(leaveDays)', type: 'number' },
        { value: 'leaveHours', label: 'leaveHours(leaveHours)', type: 'number' }
      ]
      const seen = new Set()
      const fields = defaults.concat(this.readProcessFieldOptions())
      return fields.filter(item => {
        if (!item || !item.value || seen.has(item.value)) {
          return false
        }
        seen.add(item.value)
        return true
      })
    }
  },
  watch: {
    value(val) {
      if (!this.modeler || this.syncing || val === this.currentXml) {
        return
      }
      this.openDiagram(val)
    },
    userTaskApproverType(newType) {
      if (newType === 'role' && this.roleOptions.length === 0) {
        this.loadRoleOptions()
      }
    }
  },
  mounted() {
    this.initModeler()
  },
  beforeDestroy() {
    if (this.modeler) {
      this.modeler.off('commandStack.changed', this.handleModelChange)
      this.modeler.off('import.done', this.handleModelChange)
      this.modeler.off('selection.changed', this.handleSelectionChanged)
      this.modeler.off('element.changed', this.handleElementChanged)
      this.modeler.destroy()
      this.modeler = null
    }
  },
  methods: {
    initModeler() {
      this.modeler = new BpmnModeler({
        container: this.$refs.canvas,
        moddleExtensions: {
          flowable: flowableModdle,
          lx: lingxiModdle
        }
      })
      this.modeler.on('commandStack.changed', this.handleModelChange)
      this.modeler.on('import.done', this.handleModelChange)
      this.modeler.on('selection.changed', this.handleSelectionChanged)
      this.modeler.on('element.changed', this.handleElementChanged)
      this.openDiagram(this.value)
    },
    loadRoleOptions() {
      listRole({ pageNum: 1, pageSize: 1000, status: '0' }).then(res => {
        const rows = (res && res.rows) || []
        this.roleOptions = rows
          .map(item => ({
            value: item && item.roleKey ? String(item.roleKey) : '',
            label: item && item.roleName && item.roleKey ? `${item.roleName} (${item.roleKey})` : (item && item.roleName ? item.roleName : '')
          }))
          .filter(item => item.value)
      }).catch(() => {
        this.roleOptions = []
      })
    },
    openDiagram(xml) {
      const normalizedXml = this.normalizeXmlInput(xml)
      const source = normalizedXml && normalizedXml.trim() ? normalizedXml : this.buildDefaultXml()
      this.modeler.importXML(source).then(({ warnings }) => {
        if (warnings && warnings.length) {
          console.warn('[BpmnEditor] import warnings:', warnings)
        }
        this.currentXml = source
        this.fitViewport()
        this.focusProcess()
        this.emitXml(source)
      }).catch((error) => {
        console.error('[BpmnEditor] import failed, fallback to default xml.', error)
        if (this.$message) {
          this.$message.warning('Process XML import failed, fallback to default process.')
        }
        const fallback = this.buildDefaultXml()
        this.modeler.importXML(fallback).then(() => {
          this.currentXml = fallback
          this.fitViewport()
          this.focusProcess()
          this.emitXml(fallback)
        }).catch((fallbackError) => {
          console.error('[BpmnEditor] fallback import failed.', fallbackError)
          if (this.$message) {
            this.$message.error('Default process import failed.')
          }
        })
      })
    },
    normalizeXmlInput(xml) {
      if (typeof xml !== 'string') {
        return ''
      }
      let value = xml
      const trimmed = value.trim()

      // Handle XML wrapped as a JSON string
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        try {
          const parsed = JSON.parse(trimmed)
          if (typeof parsed === 'string') {
            value = parsed
          }
        } catch (e) {
          // ignore
        }
      }

      // Handle escaped newlines and escaped quotes
      if (value.includes('\\n') || value.includes('\\"') || value.includes('\\r\\n')) {
        value = value
          .replace(/\\r\\n/g, '\n')
          .replace(/\\n/g, '\n')
          .replace(/\\"/g, '"')
      }
      return value
    },
    handleModelChange() {
      this.saveXml().then(xml => {
        this.emitXml(xml)
      })
    },
    saveXml() {
      if (!this.modeler) {
        return Promise.resolve(this.currentXml)
      }
      return this.modeler.saveXML({ format: true }).then(({ xml }) => {
        this.currentXml = xml
        return xml
      })
    },
    getXml() {
      // 在保存XML前，先持久化当前编辑的字段配置
      if (this.isProcess) {
        this.persistFieldDraft('process')
      } else if (this.isUserTask) {
        this.persistFieldDraft('userTask')
      }
      return this.saveXml()
    },
    handleReset() {
      this.openDiagram('')
    },
    handleFit() {
      this.fitViewport()
    },
    handleSelectionChanged(event) {
      const next = event.newSelection && event.newSelection.length ? event.newSelection[0] : this.getProcessElement()
      this.selectedElement = next || null
      this.selectionVersion++
      this.syncFieldDrafts()
      this.syncSequenceBuilder()
    },
    handleElementChanged(event) {
      if (!this.selectedElement || !event || !event.element) {
        return
      }
      if (event.element.id === this.selectedElement.id) {
        this.selectedElement = event.element
        this.selectionVersion++
        this.syncFieldDrafts()
        this.syncSequenceBuilder()
      }
    },
    focusProcess() {
      this.$nextTick(() => {
        const processElement = this.getProcessElement()
        if (!processElement) {
          return
        }
        const selection = this.modeler.get('selection')
        selection.select(processElement)
      })
    },
    getProcessElement() {
      if (!this.modeler) {
        return null
      }
      const elementRegistry = this.modeler.get('elementRegistry')
      return elementRegistry.find(item => is(item, 'bpmn:Process'))
    },
    fitViewport() {
      if (!this.modeler) {
        return
      }
      this.$nextTick(() => {
        const canvas = this.modeler.get('canvas')
        canvas.zoom('fit-viewport', 'auto')
      })
    },
    emitXml(xml) {
      this.syncing = true
      this.$emit('input', xml)
      this.$emit('change', xml)
      this.$nextTick(() => {
        this.syncing = false
      })
    },
    readAttr(name) {
      if (!this.selectedBo || typeof this.selectionVersion === 'undefined') {
        return ''
      }
      return this.selectedBo.get(name) || ''
    },
    updateId(value) {
      this.updateProperties({ id: value })
    },
    updateName(value) {
      this.updateProperties({ name: value })
    },
    updateUserTaskAttr(name, value) {
      const nextProperties = { [name]: value }
      if (name === 'lx:approverType' || name === 'lx:approverValue') {
        Object.assign(nextProperties, this.buildFlowableAssignment(
          name === 'lx:approverType' ? value : this.userTaskApproverType,
          name === 'lx:approverValue' ? value : this.userTaskApproverValue
        ))
      }
      this.updateProperties(nextProperties)
    },
    updateSequenceAttr(name, value) {
      this.updateProperties({ [name]: value })
    },
    updateSequenceCondition(value) {
      if (!this.selectedElement || !this.isSequenceFlow) {
        return
      }
      const modeling = this.modeler.get('modeling')
      const moddle = this.modeler.get('moddle')
      const expression = value
        ? moddle.create('bpmn:FormalExpression', { body: value })
        : null
      modeling.updateProperties(this.selectedElement, {
        conditionExpression: expression
      })
      this.syncSequenceBuilder()
    },
    updateSequenceBuilder(field, value) {
      this.sequenceBuilder = {
        ...this.sequenceBuilder,
        [field]: value
      }
    },
    applySequenceBuilder() {
      if (!this.sequenceBuilder.field || !this.sequenceBuilder.operator || this.sequenceBuilder.value === '') {
        return
      }
      const fieldMeta = this.sequenceFieldOptions.find(item => item.value === this.sequenceBuilder.field)
      const fieldType = fieldMeta && fieldMeta.type ? fieldMeta.type : 'text'
      const rawValue = String(this.sequenceBuilder.value).trim()
      const formattedValue = fieldType === 'number'
        ? rawValue
        : `'${rawValue.replace(/'/g, "\\'")}'`
      const expression = `\${${this.sequenceBuilder.field} ${this.sequenceBuilder.operator} ${formattedValue}}`
      this.updateSequenceCondition(expression)
      if (!this.sequenceConditionSummary) {
        const operatorText = this.conditionOperatorOptions.find(item => item.value === this.sequenceBuilder.operator)
        this.updateSequenceAttr('lx:conditionSummary', `${fieldMeta ? fieldMeta.label : this.sequenceBuilder.field}${operatorText ? operatorText.label : this.sequenceBuilder.operator}${rawValue}`)
      }
    },
    syncSequenceBuilder() {
      if (!this.isSequenceFlow) {
        this.sequenceBuilder = { field: '', operator: '', value: '' }
        return
      }
      this.sequenceBuilder = this.parseSequenceCondition(this.sequenceCondition)
    },
    parseSequenceCondition(value) {
      const text = String(value || '').trim()
      const matched = text.match(/^\$\{\s*([A-Za-z_][\w]*)\s*(==|!=|>=|<=|>|<)\s*(.+?)\s*\}$/)
      if (!matched) {
        return { field: '', operator: '', value: '' }
      }
      let conditionValue = matched[3].trim()
      if ((conditionValue.startsWith("'") && conditionValue.endsWith("'")) || (conditionValue.startsWith('"') && conditionValue.endsWith('"'))) {
        conditionValue = conditionValue.substring(1, conditionValue.length - 1)
      }
      return {
        field: matched[1],
        operator: matched[2],
        value: conditionValue
      }
    },
    readProcessFieldOptions() {
      const processElement = this.getProcessElement()
      if (!processElement) {
        return []
      }
      const processBo = getBusinessObject(processElement)
      if (!processBo) {
        return []
      }
      const fields = this.parseFieldConfig(processBo.get('lx:formFields'))
      return fields.map(item => ({
        value: item.code,
        label: `${item.label || item.code}(${item.code})`,
        type: item.type || 'text'
      }))
    },
    updateProperties(properties) {
      if (!this.selectedElement || !this.modeler) {
        return
      }
      const modeling = this.modeler.get('modeling')
      modeling.updateProperties(this.selectedElement, properties)
    },
    buildFlowableAssignment(approverType, approverValue) {
      const value = approverValue || ''
      const trimmedValue = value.trim()
      const assignment = {
        'flowable:assignee': '',
        'flowable:candidateUsers': '',
        'flowable:candidateGroups': ''
      }
      if (!trimmedValue) {
        return assignment
      }
      if (approverType === 'user' || approverType === 'expression') {
        assignment['flowable:assignee'] = trimmedValue
        return assignment
      }
      if (approverType === 'role') {
        assignment['flowable:candidateGroups'] = trimmedValue
        return assignment
      }
      if (approverType === 'dept') {
        assignment['flowable:candidateGroups'] = `dept:${trimmedValue}`
      }
      return assignment
    },
    syncFieldDrafts() {
      if (this.isProcess) {
        this.processFieldDraft = this.parseFieldConfig(this.readAttr('lx:formFields'))
      } else {
        this.processFieldDraft = []
      }
      if (this.isUserTask) {
        this.userTaskFieldDraft = this.parseFieldConfig(this.readAttr('lx:formFields'))
      } else {
        this.userTaskFieldDraft = []
      }
    },
    addFieldDraft(scope) {
      const key = scope === 'process' ? 'processFieldDraft' : 'userTaskFieldDraft'
      this[key] = this[key].concat([this.createEmptyField()])
    },
    removeFieldDraft(scope, index) {
      const key = scope === 'process' ? 'processFieldDraft' : 'userTaskFieldDraft'
      const next = this[key].slice()
      next.splice(index, 1)
      this[key] = next
    },
    updateFieldDraft(scope, index, field, value) {
      const key = scope === 'process' ? 'processFieldDraft' : 'userTaskFieldDraft'
      const next = this[key].slice()
      next[index] = {
        ...next[index],
        [field]: value
      }
      this[key] = next
    },
    persistFieldDraft(scope) {
      const rows = scope === 'process' ? this.processFieldDraft : this.userTaskFieldDraft
      const payload = JSON.stringify(this.normalizeFieldRows(rows))
      if (scope === 'process') {
        this.updateProperties({ 'lx:formFields': payload })
        return
      }
      this.updateUserTaskAttr('lx:formFields', payload)
    },
    parseFieldConfig(value) {
      if (!value) {
        return []
      }
      try {
        const parsed = JSON.parse(value)
        if (Array.isArray(parsed)) {
          return parsed.map(item => ({
            code: item.code || '',
            label: item.label || '',
            type: item.type || 'text',
            required: Boolean(item.required),
            displayStage: item.displayStage || 'both',
            approveReadonly: item.approveReadonly !== false,
            optionsText: this.stringifyFieldOptions(item.options)
          }))
        }
      } catch (error) {
        return String(value)
          .split('\n')
          .map(line => line.trim())
          .filter(Boolean)
          .map(line => {
            const [code = '', type = 'text', label = ''] = line.split(':')
            return {
              code,
              label,
              type,
              required: false,
              displayStage: 'both',
              approveReadonly: true,
              optionsText: ''
            }
          })
      }
      return []
    },
    normalizeFieldRows(rows) {
      return rows
        .map(item => ({
          code: (item.code || '').trim(),
          label: (item.label || '').trim(),
          type: item.type || 'text',
          required: Boolean(item.required),
          displayStage: item.displayStage || 'both',
          approveReadonly: item.approveReadonly !== false,
          options: item.type === 'select' ? this.parseFieldOptions(item.optionsText) : []
        }))
        .filter(item => item.code || item.label)
    },
    createEmptyField() {
      return {
        code: '',
        label: '',
        type: 'text',
        required: false,
        displayStage: 'both',
        approveReadonly: true,
        optionsText: ''
      }
    },
    parseFieldOptions(value) {
      return String(value || '')
        .split('\n')
        .map(line => line.trim())
        .filter(Boolean)
        .map(line => {
          const parts = line.split('|')
          const optionValue = (parts[0] || '').trim()
          const optionLabel = (parts[1] || parts[0] || '').trim()
          return {
            value: optionValue,
            label: optionLabel
          }
        })
        .filter(item => item.value || item.label)
    },
    stringifyFieldOptions(options) {
      if (!Array.isArray(options) || !options.length) {
        return ''
      }
      return options
        .map(item => {
          const value = item && item.value ? String(item.value).trim() : ''
          const label = item && item.label ? String(item.label).trim() : value
          return value ? `${value}|${label}` : ''
        })
        .filter(Boolean)
        .join('\n')
    },    buildDefaultXml() {
      const processId = this.escapeXml(this.processKey || 'oa_process')
      const processName = this.escapeXml(this.processName || 'OA Process')
      return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:flowable="http://flowable.org/bpmn"
  xmlns:lx="http://lingxi.com/schema/bpmn"
  id="Definitions_1"
  targetNamespace="http://lingxi.com/bpmn">
  <bpmn:process id="${processId}" name="${processName}" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="Activity_1" name="Approve">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Activity_1" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Activity_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${processId}">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="120" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1_di" bpmnElement="Activity_1">
        <dc:Bounds x="290" y="98" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="470" y="120" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="216" y="138" />
        <di:waypoint x="290" y="138" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="390" y="138" />
        <di:waypoint x="470" y="138" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
    },
    escapeXml(value) {
      return String(value)
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
    }
  }
}
</script>

<style scoped>
.bpmn-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #f6f8fb;
}

.bpmn-editor__main {
  display: flex;
  height: 520px;
}

.bpmn-editor__workspace {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.bpmn-editor__toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.bpmn-editor__canvas {
  flex: 1;
  min-height: 600px;
  background-image:
    linear-gradient(90deg, rgba(18, 52, 86, 0.04) 1px, transparent 1px),
    linear-gradient(rgba(18, 52, 86, 0.04) 1px, transparent 1px);
  background-size: 24px 24px;
}

.bpmn-editor__panel {
  width: 440px;
  border-left: 1px solid #e4e7ed;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.bpmn-editor__panel-title {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.bpmn-editor__panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 10px 16px 8px;
  max-height: 480px;
}

.bpmn-editor__empty,
.bpmn-editor__hint {
  color: #909399;
  line-height: 1.7;
  font-size: 13px;
}

.bpmn-editor__empty {
  padding: 16px;
}

.bpmn-editor__field-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.bpmn-editor__field-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
  padding: 10px 6px 0px 0px;
}

.bpmn-editor__field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
}

.bpmn-editor__field-grid .el-form-item {
  margin-bottom: 0;
  margin-left: 0;
  padding: 0;
  border: none;
}

.bpmn-editor__field-grid .el-form-item__label {
  font-size: 12px;
  line-height: 1.4;
  padding: 0 !important;
  margin: 0 !important;
  float: none;
  display: block;
  text-align: left;
  width: auto !important;
  max-width: none !important;
  box-sizing: border-box;
}

.bpmn-editor__field-grid .el-form-item__content {
  margin-left: 0 !important;
  margin-right: 0 !important;
  padding: 0 !important;
  line-height: 1;
}

.bpmn-editor__panel-body .el-input,
.bpmn-editor__panel-body .el-select {
  width: 100%;
}

.bpmn-editor__field-grid .el-input__inner {
  padding-left: 8px;
  padding-right: 8px;
}

.bpmn-editor__field-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.bpmn-editor__field-options {
  margin-top: 8px;
}

.bpmn-editor__condition-builder {
  display: grid;
  grid-template-columns: 1fr 120px 1fr;
  gap: 10px;
}

.bpmn-editor__condition-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
}

.bpmn-editor__condition-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

::v-deep .djs-connection path {
  stroke: #60A5FA !important;
  stroke-width: 2px !important;
}

::v-deep .djs-connection marker path {
  stroke: #60A5FA !important;
  fill: #60A5FA !important;
}

::v-deep .djs-connection:hover path,
::v-deep .djs-connection.selected path {
  stroke: #3B82F6 !important;
  stroke-width: 3px !important;
}

::v-deep .djs-connection:hover marker path,
::v-deep .djs-connection.selected marker path {
  stroke: #3B82F6 !important;
  fill: #3B82F6 !important;
}

::v-deep .djs-outline {
  stroke: rgba(59, 130, 246, 0.4) !important;
  stroke-width: 2px !important;
}

::v-deep .djs-element.selected .djs-outline {
  stroke: #3B82F6 !important;
  stroke-width: 2px !important;
}

::v-deep .djs-shape > g > rect,
::v-deep .djs-shape > g > polygon {
  stroke: #3B82F6 !important;
  fill: #F8FAFC !important;
}

::v-deep .djs-shape > g > path {
  stroke: #3B82F6 !important;
  fill: #F8FAFC !important;
}



::v-deep .djs-shape .djs-visual > :nth-child(1) {
  stroke: #3B82F6 !important;
  fill: #F8FAFC !important;
}

::v-deep .djs-label {
  fill: #334155 !important;
  color: #334155 !important;
  font-size: 12px !important;
  font-weight: normal !important;
}

::v-deep .bpmn-editor__canvas svg text,
::v-deep .bpmn-editor__canvas svg tspan {
  fill: #334155 !important;
  font-size: 12px !important;
  font-weight: normal !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif !important;
}

::v-deep .djs-shape text,
::v-deep .djs-shape tspan,
::v-deep .djs-connection text,
::v-deep .djs-connection tspan {
  fill: #334155 !important;
  font-size: 12px !important;
  font-weight: normal !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif !important;
}

::v-deep .bpmn-editor__panel-body .el-textarea__inner {
  background: rgba(30, 41, 59, 0.7) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
}

::v-deep .bpmn-editor__panel-body .el-input__inner {
  background: rgba(30, 41, 59, 0.7) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
}

::v-deep .bpmn-editor__panel-body .el-select .el-input__inner {
  background: rgba(30, 41, 59, 0.7) !important;
}

::v-deep .bpmn-editor__panel-body .el-input__inner::placeholder,
::v-deep .bpmn-editor__panel-body .el-textarea__inner::placeholder {
  color: #64748B !important;
}
</style>

